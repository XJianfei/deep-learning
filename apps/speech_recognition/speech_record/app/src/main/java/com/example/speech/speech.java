package com.example.speech;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class speech extends Activity {

    private static final String TAG = "SpeechRecognize";
    private Button startButton, stopButton, playButton;
    private int bufferSizeInBytes = 0;//缓冲区大小
    //音频获取来源
    private int audioSource = MediaRecorder.AudioSource.MIC;
    //设置音频的采样率，44100是目前的标准，但是某些设备仍然支持22050,16000,11025
    private static int sampleRateInHz = 16000;
    //设置音频的录制声道，CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道CHANNEL_IN_MONO
    private static int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    //设置音频数据格式:PCM 16位每个样本，保证设备支持。PCM 8位每个样本，不一定能得到设备的支持。
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //AudioName裸音频数据文件
    private static final String AudioName = "/sdcard/record.raw";
    //NewAudioName可播放的音频文件
    private static final String NewAudioName = "/sdcard/record.wav";

    private AudioRecord audioRecord;

    //播放音频
    private MediaPlayer mediaPlayer;

    private boolean isRecord = false;//设置录制状态

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech);
        init();
    }

    private void init() {
        startButton = (Button)findViewById(R.id.startButton);
        stopButton = (Button)findViewById(R.id.stopButton);
        playButton = (Button)findViewById(R.id.playButton);

        creatAudioRecord();
        startButton.setOnClickListener(new AudioRecordLinstener());
        stopButton.setOnClickListener(new AudioRecordLinstener());
        playButton.setOnClickListener(new AudioRecordLinstener());
    }

    private void creatAudioRecord() {
        //根据AudioRecord的音频采样率、音频录制声道、音频数据格式获取缓冲区大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        Log.d(TAG, "creatAudioRecord-->bufferSizeInBytes="+bufferSizeInBytes);

        //根据音频获取来源、音频采用率、音频录制声道、音频数据格式和缓冲区大小来创建AudioRecord对象
        audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);

        //创建播放实例
        mediaPlayer = new MediaPlayer();

    }

    private class AudioRecordLinstener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(v == startButton)
            {
                startAudioRecord();
            }
            if(v == stopButton)
            {
                stopAudioRecord();
            }
            if(v == playButton)
            {
                playMusic();
            }
        }
    }

    private final class PrepareListener implements MediaPlayer.OnPreparedListener
    {

        @Override
        public void onPrepared(MediaPlayer mp)
        {
            // TODO Auto-generated method stub
            mediaPlayer.start();//开始播放
        }

    }


    private void playMusic() {
        File file = new File(NewAudioName);
        if(file.exists())
        {
            try
            {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(NewAudioName);
                mediaPlayer.prepare();//进行数据缓冲

                mediaPlayer.setOnPreparedListener(new PrepareListener());
            } catch (IllegalArgumentException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SecurityException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 开始录制音频
     */
    private void startAudioRecord() {
        audioRecord.startRecording();//开始录制
        isRecord = true;
        new AudioRecordThread().start();//开启线程来把录制的音频数据保留下来

    }

    private void stopAudioRecord() {
        close();
    }
    private void close()
    {
        if (audioRecord != null)
        {
            System.out.println("stopRecord");
            isRecord = false;//停止文件写入
            audioRecord.stop();
            audioRecord.release();//释放资源
            audioRecord = null;
        }
    }

    private class AudioRecordThread extends Thread{
        @Override
        public void run()
        {
            super.run();
            writeDataToFile();//把录制的音频裸数据写入到文件中去
            copyWaveFile(AudioName, NewAudioName);//给裸数据加上头文件
        }

    }
    /**
     * 把录制的音频裸数据写入到文件中去
     * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
     * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
     * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
     */
    private void writeDataToFile()
    {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        byte[] audioData = new byte[bufferSizeInBytes];
        int readSize = 0;
        FileOutputStream fos = null;
        File file = new File(AudioName);
        if(file.exists())
            file.delete();
        try
        {
            fos = new FileOutputStream(file);//获取一个文件的输出流
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while(isRecord == true)
        {
            readSize = audioRecord.read(audioData, 0, bufferSizeInBytes);
            Log.d(TAG, "readSize ="+readSize);
            if(AudioRecord.ERROR_INVALID_OPERATION != readSize)
            {
                try
                {
                    fos.write(audioData);
                } catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        try
        {
            fos.close();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void copyWaveFile(String inFileName, String outFileName)
    {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = sampleRateInHz;
        int channels = 2;
        long byteRate = 16 * sampleRateInHz * channels / 8;

        byte[] data = new byte[bufferSizeInBytes];
        try
        {
            in = new FileInputStream(inFileName);
            out = new FileOutputStream(outFileName);

            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            while(in.read(data) != -1)
            {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。
     * 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有
     * 自己特有的头文件。
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    @Override
    protected void onDestroy()
    {
        close();
        if(mediaPlayer != null)
        {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

}
