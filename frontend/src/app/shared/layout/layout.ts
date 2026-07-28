import { Component } from '@angular/core';
import { Navbar } from '../navbar/navbar';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-layout',
  imports: [ Navbar, RouterOutlet ],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class Layout {}
