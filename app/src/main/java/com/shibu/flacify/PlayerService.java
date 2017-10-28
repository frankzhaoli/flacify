package com.shibu.flacify;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.IntDef;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.io.IOException;

public class PlayerService extends Service
{
    MediaPlayer mediaPlayer=new MediaPlayer();
    private final IBinder nBinder=new MyBinder();

    public class MyBinder extends Binder
    {
        PlayerService getService()
        {
            return PlayerService.this;
        }
    }

    public PlayerService()
    {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(intent.getStringExtra("url")!=null)
        {
            playStream(intent.getStringExtra("url"));
        }

        if(intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION))
        {
            Log.i("info", "Start foreground service");
            showNotification();
        }
        else if(intent.getAction().equals(Constants.ACTION.PREV_ACTION))
        {
            Log.i("info", "Prev pressed");
        }
        else if(intent.getAction().equals(Constants.ACTION.PLAY_ACTION))
        {
            Log.i("info", "Play pressed");
            togglePlayer();
        }
        else if(intent.getAction().equals(Constants.ACTION.NEXT_ACTION))
        {
            Log.i("info", "Next pressed");
        }
        else if(intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION))
        {
            Log.i("info", "Stop foreground received");
            stopForeground(true);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void showNotification()
    {
        Intent notificationIntent=new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
        //notificationIntent.setFlags((Intent.FLAG_ACTIVITY_CLEAR_TASK));
        PendingIntent pendingIntent=PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent previousIntent=new Intent(this, PlayerService.class);
        previousIntent.setAction(Constants.ACTION.PREV_ACTION);
        PendingIntent ppreviousIntent=PendingIntent.getService(this, 0, previousIntent, 0);

        Intent playIntent=new Intent(this, PlayerService.class);
        playIntent.setAction(Constants.ACTION.PLAY_ACTION);
        PendingIntent pplayIntent=PendingIntent.getService(this, 0, playIntent, 0);

        Intent nextIntent=new Intent(this, PlayerService.class);
        nextIntent.setAction(Constants.ACTION.NEXT_ACTION);
        PendingIntent pnextIntent=PendingIntent.getService(this, 0, nextIntent, 0);

        Bitmap icon= BitmapFactory.decodeResource(getResources(), R.drawable.symbol);

        int playPauseButtonId=android.R.drawable.ic_media_play;
        String strPlaying="Play";

        if(mediaPlayer!=null && mediaPlayer.isPlaying())
        {
            playPauseButtonId=android.R.drawable.ic_media_pause;
            strPlaying="Pause";
        }

        Notification notification=new NotificationCompat.Builder(this)
                .setContentTitle("Music Player")
                .setTicker("Playing Music")
                .setContentText("My Song")
                .setSmallIcon(R.drawable.symbol)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_previous, "Previous", ppreviousIntent)
                .addAction(playPauseButtonId, strPlaying, pplayIntent)
                .addAction(android.R.drawable.ic_media_next, "Next", pnextIntent)
                .build();
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return nBinder;
    }

    public void playStream(String url)
    {
        if(mediaPlayer!=null)
        {
            try
            {
                mediaPlayer.stop();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            mediaPlayer=null;
        }

        mediaPlayer=new MediaPlayer();
        //optimize this later
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try
        {
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
            {
                @Override
                public void onPrepared(MediaPlayer mp)
                {
                    playPlayer();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    flipPlayPauseButton(false);
                }
            });
            mediaPlayer.prepareAsync();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public void pausePlayer()
    {
        try
        {
            mediaPlayer.pause();
            flipPlayPauseButton(false);
            showNotification();
            unregisterReceiver(noisyAudioStreamReceiver);
        }
        catch(Exception e)
        {
            Log.d("Exception:", "failed to pause media player");
        }
    }

    public void playPlayer()
    {
        try
        {
            getAudioFocusAndPlay();
            flipPlayPauseButton(true);
            showNotification();
        }
        catch(Exception e)
        {
            Log.d("Exception:", "failed to play media player");
        }
    }

    public void flipPlayPauseButton(boolean isPlaying)
    {
        Intent intent=new Intent("changePlayButton");
        intent.putExtra("isPlaying", isPlaying);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void togglePlayer()
    {
        try {
            if (mediaPlayer.isPlaying()) {
                pausePlayer();
            } else {
                playPlayer();
            }
        } catch (Exception e) {
            Log.d("Exception:", "failed to toggle media player");
        }
    }

    private AudioManager am;
    private boolean playingBeforeInterrupt=false;

    public void  getAudioFocusAndPlay()
    {
        am=(AudioManager) this.getBaseContext().getSystemService(Context.AUDIO_SERVICE);
        //optimize the deprecated function
        int result=am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if(result== AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        {
            mediaPlayer.start();
            registerReceiver(noisyAudioStreamReceiver, intentFiler);
        }
    }

    AudioManager.OnAudioFocusChangeListener afChangeListener=new AudioManager.OnAudioFocusChangeListener()
    {
        @Override
        public void onAudioFocusChange(int focusChange)
        {
            if(focusChange==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            {
                if(mediaPlayer.isPlaying())
                {
                    playingBeforeInterrupt=true;
                }
                else
                {
                    playingBeforeInterrupt=false;
                }
                pausePlayer();
            }
            else if(focusChange==AudioManager.AUDIOFOCUS_GAIN)
            {
                if(playingBeforeInterrupt)
                {
                    playPlayer();
                }

            }
            else if(focusChange==AudioManager.AUDIOFOCUS_LOSS)
            {
                pausePlayer();
                am.abandonAudioFocus(afChangeListener);
            }
            //have to use all cases of AUDIOFOCUS
        }
    };

    //audio reroute
    private class NoisyAudioStreamReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
            {
                Log.i("HEADPHONE EVENT", "phones unplugged");
                pausePlayer();
            }
        }
    }

    private IntentFilter intentFiler=new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private NoisyAudioStreamReceiver noisyAudioStreamReceiver=new NoisyAudioStreamReceiver();
}
