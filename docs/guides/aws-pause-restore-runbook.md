# AWS Pause / Restore Runbook

Step-by-step procedure for pausing TopTrader's AWS deployment to near-zero cost, and bringing it back. The *why* is in [ADR 0051](../adr/0051-pause-aws-infrastructure.md). Resource IDs and settings referenced below come from [aws-infrastructure-implementation.md](../tasks/completed/aws-infrastructure-implementation.md) - check there for anything not repeated here.

All console steps are in region **us-east-2** (Ohio) unless noted. Do them signed in as `toptrader-admin`, not root.

**What gets deleted:** EC2 instance, RDS instance, Elastic IP.
**What stays:** S3 bucket, both CloudFront distributions, ACM cert, Route 53 zone + domain, SSM parameters, IAM roles/policies/OIDC provider, SNS topic, CloudWatch log group + alarm, Budget, SES identity.

---

## Pausing

### 0. Before touching AWS

- [ ] Merge the PR that adds the `DEPLOY_ENABLED` gate to `ci.yml`. Leave the `DEPLOY_ENABLED` repo variable **unset** (or set it to `false` under GitHub repo **Settings → Secrets and variables → Actions → Variables**). From here on, merges to `main` run CI but skip both deploy jobs.
- [ ] Confirm the domain `toptrader.dev` is on auto-renew: **Route 53 → Registered domains → toptrader.dev**. If it lapses, someone else can register it.

### 1. Create an AMI of the EC2 instance

This preserves all the manual server setup (systemd units, health-check timer, `deploy` user, sshd port 3333, fail2ban, CloudWatch agent, `/var/log/toptrader`).

1. **EC2 → Instances**, select `i-0918607b17b50cbe8`.
2. Before imaging, write down (or screenshot) from the instance's details tabs: instance type (`t4g.micro`), key pair name, security group (`toptrader-ec2-sg`), IAM role (`toptrader-ec2-role`), subnet/AZ (`us-east-2b`). An AMI does **not** remember the IAM role or security group - you'll re-pick them on restore.
3. **Actions → Image and templates → Create image.**
   - Image name: `toptrader-ec2-paused-YYYY-MM-DD`
   - Leave "Reboot instance" checked (gives a consistent disk image; the site is about to go down anyway).
   - Leave the root volume as-is.
4. **EC2 → AMIs**: wait until the new AMI's status is **Available** (a few minutes). Don't continue until it is.

### 2. Terminate the EC2 instance

1. **EC2 → Instances**, select the instance → **Instance state → Terminate (delete) instance**.
   - If it refuses, termination protection is on: **Actions → Instance settings → Change termination protection**, disable, retry.
2. **EC2 → Volumes**: once termination finishes, confirm there's no leftover `available` (unattached) volume from this instance. If there is one, delete it - the AMI's snapshot already has its data.

### 3. Release the Elastic IP

1. **EC2 → Elastic IPs**, select the address (`18.226.207.9`).
2. If it still shows an association, **Actions → Disassociate Elastic IP address** first.
3. **Actions → Release Elastic IP addresses → Release.**

### 4. Delete RDS with a final snapshot

1. **RDS → Databases**, select `toptrader`.
2. If **Deletion protection** is enabled (Configuration tab): **Modify → uncheck Deletion protection → Apply immediately**, wait for it to return to Available.
3. **Actions → Delete.**
   - **Check** "Create final snapshot", name it `toptrader-final-YYYY-MM-DD`.
   - **Uncheck** "Retain automated backups" - those cost extra and the final snapshot is all you need.
   - Type `delete me` and confirm.
4. **RDS → Snapshots → Manual**: confirm the final snapshot appears and reaches **Available**.

### 5. Verify and tidy up

- [ ] **EC2 → Instances**: no running/stopped instances. **Volumes**: none unattached. **Elastic IPs**: none.
- [ ] **RDS → Databases**: empty. **Snapshots**: `toptrader-final-...` present.
- [ ] **EC2 → AMIs**: `toptrader-ec2-paused-...` present (its backing EBS snapshot is under **EC2 → Snapshots** - don't delete that).
- [ ] Visit `https://app.toptrader.dev`: the page loads, but API calls fail. Expected.
- [ ] Optional: lower the AWS Budget to something like $5 so a forgotten resource gets flagged quickly.
- [ ] A few days later: **Billing → Bills** (or Cost Explorer, grouped by service) - confirm EC2-Instance / RDS-instance charges have stopped and only small snapshot/S3/Route 53 charges remain.

---

## Restoring

Order matters: database first (the app connects to it on startup), then EC2, then point CloudFront and CI at the new server.

### 1. Restore RDS from the snapshot

1. **RDS → Snapshots → Manual**, select `toptrader-final-...` → **Actions → Restore snapshot.**
2. Settings (match the original instance):
   - DB instance identifier: **`toptrader`** - reusing the same name gives the same endpoint hostname, so the `/toptrader/prod/spring-datasource-url` SSM parameter doesn't need to change.
   - Instance class: `db.t4g.micro`, Single-AZ, storage gp3.
   - VPC: default. Public access: **No**.
   - Security group: **remove `default`, select `toptrader-rds-sg`.** The restore wizard defaults to the VPC's default group - leaving it means EC2 can't connect.
3. Wait for status **Available**, then check its **Endpoint** against the `spring-datasource-url` value in **Systems Manager → Parameter Store**. If they differ, update the parameter.
4. The master password is whatever it was when the snapshot was taken (unchanged from the `spring-datasource-password` SSM parameter).

### 2. Allocate a new Elastic IP

1. **EC2 → Elastic IPs → Allocate Elastic IP address → Allocate.**
2. Note the new IP and its public DNS name (format `ec2-A-B-C-D.us-east-2.compute.amazonaws.com`, where `A-B-C-D` is the IP with dashes).

### 3. Launch EC2 from the AMI

1. **EC2 → AMIs**, select `toptrader-ec2-paused-...` → **Launch instance from AMI.**
2. Settings:
   - Name: `toptrader`
   - Instance type: `t4g.micro`
   - Key pair: the same one as before.
   - Network: default VPC, a public subnet with auto-assign public IP; security group: **select existing `toptrader-ec2-sg`** (don't let it create a new one).
   - **Advanced details → IAM instance profile: `toptrader-ec2-role`.** Without this, `fetch-secrets.sh` can't read SSM and the app won't start.
3. Launch, wait for **Running** and 2/2 status checks.
4. **EC2 → Elastic IPs**, select the new address → **Actions → Associate Elastic IP address** → pick the new instance.

### 4. Verify the app on the instance

1. SSH in on port 3333 using the new Elastic IP DNS name. (Your local `known_hosts` has the old address - expect a new-host prompt, not a changed-key warning, since the host key came across in the AMI.)
2. `sudo systemctl status toptrader.service` - should be active; it's enabled, so it starts on boot and fetches SSM via `ExecStartPre`.
3. `curl -s http://localhost:8080/actuator/health` → `{"status":"UP"}`. If not, check `sudo journalctl -u toptrader.service` - usual suspects are the IAM role (step 3) or the RDS security group (step 1).

### 5. Point CloudFront at the new server

1. **CloudFront → Distributions → `EBJQ07VSB22PM`** (`toptrader-backend` - *not* `E2QV9MPC8DTN65`, which is the frontend/S3 one).
2. **Origins** tab → select the origin → **Edit** → set **Origin domain** to the new Elastic IP DNS name from step 2. Type/paste it; don't pick from the dropdown (that's how the original cutover ended up pointing at a stale address). Keep HTTP only, port 8080.
3. Save, wait for the distribution's "Last modified" to finish deploying.
4. `curl https://api.toptrader.dev/actuator/health` → `200 {"status":"UP"}`.

### 6. Point CI/CD at the new server and re-enable deploys

1. On a `chore/*` branch, update both `host:` lines in `.github/workflows/ci.yml`'s `deploy-backend` job (the SCP step and the SSH step) to the new Elastic IP DNS name. PR and merge as usual.
2. Set the `DEPLOY_ENABLED` repo variable to `true` (**Settings → Secrets and variables → Actions → Variables**).
3. **Actions → CI → Run workflow** on `main` (`workflow_dispatch`) to deploy the latest frontend and backend. Both deploy jobs should pass, including the backend post-deploy smoke test. This also catches the live site up on anything merged while paused - any new Flyway migrations apply on this startup.

### 7. Re-point the status-check alarm

1. **CloudWatch → Alarms → `toptrader-ec2-status-check-failed` → Edit.**
2. Change the metric's `InstanceId` dimension to the new instance ID. Save. It should move to `OK` after its next evaluation.

### 8. Smoke test and clean up

- [ ] Full check on `https://app.toptrader.dev`: Try Demo, register, login, quote lookup, buy, sell, portfolio, transactions, performance, friends.
- [ ] Revert README's status line back to "Live at app.toptrader.dev".
- [ ] Update `docs/ROADMAP.md`'s current-focus line to drop the "paused" note.
- [ ] Once everything has run fine for a few days: deregister the paused AMI (**EC2 → AMIs → Deregister**), delete its backing snapshot (**EC2 → Snapshots**), and delete the `toptrader-final-...` RDS snapshot - otherwise they keep billing a few cents a month.
