import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { MobileHeaderComponent } from './mobile-header/mobile-header.component';
import { SidebarComponent } from './sidebar/sidebar.component';

@Component({
  selector: 'app-app-shell',
  imports: [RouterOutlet, SidebarComponent, MobileHeaderComponent],
  templateUrl: './app-shell.component.html',
})
export default class AppShellComponent {}
