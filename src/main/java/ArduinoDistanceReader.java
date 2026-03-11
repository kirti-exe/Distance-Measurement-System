import com.fazecast.jSerialComm.SerialPort;
import org.jfree.chart.ChartPanel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;

class ArduinoCOM3Reader {

    //Toggle to switch modes
    static boolean SIMULATION_MODE = true;

    //Control whether the system reads sensor data
    static boolean monitoring = true;

    public static void main(String[] args) {

        // Graph Generator
        GraphGenerator.startGraph();

        Random random = new Random();

        SerialPort port = null;
        BufferedReader reader = null;

//----------------------------------------SERIAL CONNECTION (WHILE USING ARDUINO)---------------------------------------
        if(!SIMULATION_MODE){
            port = SerialPort.getCommPort("COM3");

            port.setBaudRate(9600);
            port.setNumDataBits(8);
            port.setNumStopBits(SerialPort.ONE_STOP_BIT);
            port.setParity(SerialPort.NO_PARITY);

            if (!port.openPort()) {
                System.out.println("❌ Failed to open COM3");
                return;
            }
        }

        if(!SIMULATION_MODE){
            System.out.println("✅ Connected to COM3 Successfully!");

            reader = new BufferedReader(
                    new InputStreamReader(port.getInputStream()));
        }else{
            System.out.println("⚙ Running in SIMULATION MODE");
        }

//----------------------------------------------------MAIN LOOP---------------------------------------------------------
        // (Will start distance vs time graph)
        ChartPanel panel = GraphGenerator.startGraph();
        Dashboard.start(panel);

        while(true){

            try{
                double distance;

                if(!monitoring){
                    Thread.sleep(500);
                    continue;
                }

//---------------------------------------------------GET DISTANCE-------------------------------------------------------
                if(SIMULATION_MODE){

                    // Simulation sensor data (0 - 100)
                    distance = random.nextInt(100);
                    Thread.sleep(2000);

                }else{

                    String line = reader.readLine();
                    if(line == null || line.isEmpty()){
                        continue;
                    }
                    distance = Double.parseDouble(line.trim());
                }

//--------------------------------------------------DISPLAY DISTANCE----------------------------------------------------
                System.out.println("Distance: " + distance + " cm");

                // Upgrade graph with data
                GraphGenerator.update(distance);
//--------------------------------------------------ANALYSIS LOGIC------------------------------------------------------
                String status;

                if(distance < 10){
                    status = "critical";
                    System.out.println("ALERT: Object Too CLose !");
                }
                else if (distance < 30){
                    status = "warning";
                    System.out.println("Warning: Medium Distance");
                }
                else{
                    status = "SAFE";
                    System.out.println("Safe Distance");
                }

//--------------------------------------------------DATABASE STORAGE----------------------------------------------------
                // (Will store distance, status timestamp)
                DatabaseManager.save(distance, status);

//--------------------------------------------------GRAPH GENERATION----------------------------------------------------

//-----------------------------------------------------GUI UPDATE-------------------------------------------------------
                // (Will update live dashboard)
                GraphGenerator.update(distance);
                Dashboard.update(distance, status);
                DatabaseManager.save(distance, status);

                System.out.println("--------------------------------");
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}

/*  WHAT TO ADD NEXT
    1. Real Time Alert System
    2. Distance Threshold
    3. Stop/ Start Monitoring System
 */