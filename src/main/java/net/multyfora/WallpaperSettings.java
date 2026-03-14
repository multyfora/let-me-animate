package net.multyfora;

import java.util.prefs.Preferences;

/**
 * Stores and retrieves all linux-wallpaperengine launch settings.
 * Uses Java Preferences so settings persist across app restarts.
 */
public class WallpaperSettings {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(WallpaperSettings.class);

    // Keys
    private static final String KEY_SCALING          = "scaling";
    private static final String KEY_SCREEN            = "screen";
    private static final String KEY_FPS               = "fps";
    private static final String KEY_NO_AUDIO          = "no_audio_processing";
    private static final String KEY_SILENT            = "silent";
    private static final String KEY_VOLUME            = "volume";
    private static final String KEY_DISABLE_PARALLAX  = "disable_parallax";

    // Defaults
    public static final String DEFAULT_SCALING = "default";
    public static final String DEFAULT_SCREEN  = "";
    public static final int    DEFAULT_FPS     = 0;       // 0 = not set
    public static final int    DEFAULT_VOLUME  = 100;

    //  Getters

    public static String getScaling()         { return PREFS.get(KEY_SCALING, DEFAULT_SCALING); }
    public static String getScreen()          { return PREFS.get(KEY_SCREEN, DEFAULT_SCREEN); }
    public static int    getFps()             { return PREFS.getInt(KEY_FPS, DEFAULT_FPS); }
    public static boolean isNoAudio()         { return PREFS.getBoolean(KEY_NO_AUDIO, false); }
    public static boolean isSilent()          { return PREFS.getBoolean(KEY_SILENT, false); }
    public static int    getVolume()          { return PREFS.getInt(KEY_VOLUME, DEFAULT_VOLUME); }
    public static boolean isDisableParallax() { return PREFS.getBoolean(KEY_DISABLE_PARALLAX, false); }

    //  Setters

    public static void setScaling(String v)         { PREFS.put(KEY_SCALING, v); }
    public static void setScreen(String v)          { PREFS.put(KEY_SCREEN, v); }
    public static void setFps(int v)                { PREFS.putInt(KEY_FPS, v); }
    public static void setNoAudio(boolean v)        { PREFS.putBoolean(KEY_NO_AUDIO, v); }
    public static void setSilent(boolean v)         { PREFS.putBoolean(KEY_SILENT, v); }
    public static void setVolume(int v)             { PREFS.putInt(KEY_VOLUME, v); }
    public static void setDisableParallax(boolean v){ PREFS.putBoolean(KEY_DISABLE_PARALLAX, v); }

    /**
     * Builds the extra flags string to append to the launch command.
     * Called by RunCommand when launching a wallpaper.
     */
    public static String buildFlags() {
        StringBuilder sb = new StringBuilder();

        String scaling = getScaling();
        if (!scaling.isBlank() && !scaling.equals("default"))
            sb.append("--scaling ").append(scaling).append(" ");

        String screen = getScreen();
        if (!screen.isBlank())
            sb.append("--screen-root ").append(screen).append(" ");

        int fps = getFps();
        if (fps > 0)
            sb.append("--fps ").append(fps).append(" ");

        if (isNoAudio())
            sb.append("--no-audio-processing ");

        if (isSilent())
            sb.append("--silent ");
        else {
            int volume = getVolume();
            if (volume != DEFAULT_VOLUME)
                sb.append("--volume ").append(volume).append(" ");
        }

        if (isDisableParallax())
            sb.append("--disable-parallax ");

        return sb.toString().trim();
    }
}
