import { Native } from "./Native";

export interface NativeEnvironment {
  getExternalStorageDir(): string;
  getPackageDataDir(): string;
  getPublicDir(type: string): string;
  getDataDir(): string;
}

/**
 * @implements {NativeEnvironment}
 */
class EnvironmentClass extends Native<NativeEnvironment> {
  public constructor() {
    super(window.__environment__);
  }

  public readonly DIRECTORY_MUSIC: string = "Music";
  public readonly DIRECTORY_PODCASTS: string = "Podcasts";
  public readonly DIRECTORY_RINGTONES: string = "Ringtones";
  public readonly DIRECTORY_ALARMS: string = "Alarms";
  public readonly DIRECTORY_NOTIFICATIONS: string = "Notifications";
  public readonly DIRECTORY_PICTURES: string = "Pictures";
  public readonly DIRECTORY_MOVIES: string = "Movies";
  public readonly DIRECTORY_DOWNLOADS: string = "Download";
  public readonly DIRECTORY_DCIM: string = "DCIM";
  public readonly DIRECTORY_DOCUMENTS: string = "Documents";
  public readonly DIRECTORY_SCREENSHOTS: string = "Screenshots";
  public readonly DIRECTORY_AUDIOBOOKS: string = "Audiobooks";
  public readonly DIRECTORY_RECORDINGS: string = "Recordings";

  public getExternalStorageDir(): string {
    return this.interface.getExternalStorageDir();
  }

  public getPackageDataDir(): string {
    return this.interface.getPackageDataDir();
  }

  public getPublicDir(type: string): string {
    return this.interface.getPublicDir(type);
  }

  public getDataDir(): string {
    return this.interface.getDataDir();
  }
}

export const Environment: EnvironmentClass = new EnvironmentClass();
