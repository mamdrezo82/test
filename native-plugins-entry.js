import { registerPlugin } from '@capacitor/core';
import { LocalNotifications } from '@capacitor/local-notifications';
window.RoozYarLocalNotifications = LocalNotifications;
window.RoozYarSaveFile = registerPlugin('RoozYarSaveFile');
import { App } from '@capacitor/app';
window.RoozYarApp = App;
