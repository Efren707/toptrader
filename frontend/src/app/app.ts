import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Root application shell component. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
