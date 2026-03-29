package controller;

import model.DistanceModel;
import model.DistanceReading;
import model.AppConfig;

import javax.sound.sampled.*;
import java.io.File;

public class BeepController implements DistanceModel.ReadingListener{
    private double currentDistance = 100;
    private boolean running = false;

    Clip clip;
    private void playBeep(){
        try{
            if(clip == null || !clip.isRunning()){
                File soundFile = new File("resources/beep_sound.wav");
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void start(){
        running = true;
        Thread thread = new Thread (() -> {
            while(running){
                try{
                    int delay = getBeepDelay();
                    if(delay > 0){
                        playBeep();
                        Thread.sleep(delay);
                    }else{
                        Thread.sleep(200);
                    }
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "BeepThread");
        thread.setDaemon(true);
        thread.start();
    }

    private int getBeepDelay(){
        double critical = AppConfig.CRITICAL_THRESHOLD;
        double warning = AppConfig.WARNING_THRESHOLD;

        if(currentDistance < critical){
            return 150;     // very fast - critical
        }else if(currentDistance < critical + (warning - critical) / 2){
            return 350;     // fast = approaching critical
        }else if(currentDistance < warning){
            return 700;     // medium - warning zone
        }else{
            return 0;       // silent - safe
        }
    }

    public void stop(){
        running = false;
    }

    @Override
    public void onNewReading(DistanceReading reading){
        currentDistance = reading.getDistance();
    }
}
