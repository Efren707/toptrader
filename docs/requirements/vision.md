# Project Vision & Charter

> Status: Accepted — 2026-07-16

## Problem Statement

New and aspiring investors often want to learn how markets and trading mechanics work without risking real money. Most existing paper-trading tools are either bundled into a broker's platform (locked to that broker's UX) or are too simplistic to reflect real constraints like transaction history, portfolio valuation, and cash management. TopTrader is a standalone simulator that lets a user practice trading against real market data using virtual cash, in a self-contained app.

## Target User

Primary: the project author, as both builder and demo user, and anyone reviewing the project (recruiters/engineers) who wants to interact with a working demo.
Secondary (illustrative, not a build target for MVP): a student or new investor who wants a low-stakes way to practice buying/selling stocks.

## Goals

- Learn and demonstrate full-stack engineering practices (Spring Boot / PostgreSQL / Angular) end to end, from planning through deployed CI/CD.
- Produce a deployed, working MVP that a reviewer can actually use, not just read about.
- Build a foundation that can grow incrementally (leaderboard, price caching, rate-limiting) without a rewrite.

## Non-goals (for the project as a whole, not just MVP)

- Real money, real brokerage integration, or any regulated trading activity.
- Multi-tenant "real" production scale (this is a portfolio project, not a startup).

## Definition of Done — MVP

A deployed, publicly accessible app where a user can: register/log in, receive virtual starting cash, look up a stock's current price, buy and sell shares of that stock, and see their resulting portfolio (holdings + cash) and transaction history. Deployed on AWS via a CI/CD pipeline from the `main` branch.

## Definition of Done — Full Vision (post-MVP, incremental)

MVP, plus: a leaderboard ranking simulated portfolio performance across users, price caching to reduce external API load/cost, and rate-limiting to protect the API from abuse — each shipped as its own incremental release once MVP is stable.
