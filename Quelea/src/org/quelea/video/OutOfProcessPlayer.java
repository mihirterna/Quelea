package org.quelea.video;

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import java.awt.Canvas;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import org.quelea.utils.QueleaProperties;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_player_t;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.linux.LinuxEmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.mac.MacEmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.windows.WindowsEmbeddedMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

/**
 * Sits out of process so as not to crash the primary VM.
 * @author Michael
 */
public class OutOfProcessPlayer {

    private EmbeddedMediaPlayer mediaPlayer;

    public OutOfProcessPlayer(final long canvasId) throws Exception {

        //Lifted pretty much out of the VLCJ code
        if (RuntimeUtil.isNix()) {
            mediaPlayer = new LinuxEmbeddedMediaPlayer(LibVlcFactory.factory().synchronise().log().create().libvlc_new(1, new String[]{"--no-video-title"}), null) {

                @Override
                protected void nativeSetVideoSurface(libvlc_media_player_t mediaPlayerInstance, Canvas videoSurface) {
                    libvlc.libvlc_media_player_set_xwindow(mediaPlayerInstance, (int) canvasId);
                }
            };
        }
        else if (RuntimeUtil.isWindows()) {
            mediaPlayer = new WindowsEmbeddedMediaPlayer(LibVlcFactory.factory().synchronise().log().create().libvlc_new(1, new String[]{"--no-video-title"}), null) {

                @Override
                protected void nativeSetVideoSurface(libvlc_media_player_t mediaPlayerInstance, Canvas videoSurface) {
                    Pointer ptr = Pointer.createConstant(canvasId);
                    libvlc.libvlc_media_player_set_hwnd(mediaPlayerInstance, ptr);
                }
            };
        }
        else if (RuntimeUtil.isMac()) {
            mediaPlayer = new MacEmbeddedMediaPlayer(LibVlcFactory.factory().synchronise().log().create().libvlc_new(2, new String[]{"--no-video-title", "--vout=macosx"}), null) {

                @Override
                protected void nativeSetVideoSurface(libvlc_media_player_t mediaPlayerInstance, Canvas videoSurface) {
                    Pointer ptr = Pointer.createConstant(canvasId);
                    libvlc.libvlc_media_player_set_nsobject(mediaPlayerInstance, ptr);
                }
            };
        }
        else {
            mediaPlayer = null;
            System.exit(1);
        }

        mediaPlayer.setVideoSurface(new Canvas());

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String inputLine;

        if (!TEST_MODE) {
            //Process the input - I know this isn't very OO but it works for now...
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("open ")) {
                    inputLine = inputLine.substring("open ".length());
                    mediaPlayer.prepareMedia(inputLine);
                }
                else if (inputLine.equalsIgnoreCase("play")) {
                    mediaPlayer.play();
                }
                else if (inputLine.equalsIgnoreCase("pause")) {
                    mediaPlayer.pause();
                }
                else if (inputLine.equalsIgnoreCase("stop")) {
                    mediaPlayer.stop();
                }
                else if (inputLine.equalsIgnoreCase("playable?")) {
                    System.out.println(mediaPlayer.isPlayable());
                }
                else if (inputLine.startsWith("setTime ")) {
                    inputLine = inputLine.substring("setTime ".length());
                    mediaPlayer.setTime(Long.parseLong(inputLine));
                }
                else if (inputLine.startsWith("setMute ")) {
                    inputLine = inputLine.substring("setMute ".length());
                    mediaPlayer.mute(Boolean.parseBoolean(inputLine));
                }
                else if (inputLine.equalsIgnoreCase("mute?")) {
                    boolean mute = mediaPlayer.isMute();
                    System.out.println(mute);
                }
                else if (inputLine.equalsIgnoreCase("length?")) {
                    long length = mediaPlayer.getLength();
                    System.out.println(length);
                }
                else if (inputLine.equalsIgnoreCase("time?")) {
                    long time = mediaPlayer.getTime();
                    System.out.println(time);
                }
                else if (inputLine.equalsIgnoreCase("close")) {
                    System.exit(0);
                }
                else {
                    System.out.println("unknown command: ." + inputLine + ".");
                }
            }
        }
    }
    
    private static final boolean TEST_MODE = false;

    public static void main(String[] args) {
        if (TEST_MODE) {
            args = new String[]{"0"};
        }
        File nativeDir = new File("lib/native");
        NativeLibrary.addSearchPath("libvlc", nativeDir.getAbsolutePath());
        NativeLibrary.addSearchPath("vlc", nativeDir.getAbsolutePath());
        try (PrintStream stream = new PrintStream(new File(QueleaProperties.getQueleaUserHome(), "ooplog.txt"))) {
            System.setErr(stream); //Important, MUST redirect err stream
            OutOfProcessPlayer player = new OutOfProcessPlayer(Integer.parseInt(args[0]));
//            player.mediaPlayer.prepareMedia("dvdsimple://E:");
//            player.mediaPlayer.play();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
