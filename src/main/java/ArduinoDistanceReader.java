import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;

class ArduinoCOM3Reader {

    //Toggle to switch modes
    static boolean SIMULATION_MODE = true;

    public static void main(String[] args) {

        Random random = new Random();

        SerialPort port = null;
        BufferedReader reader = null;

//----------------------------------------SERIAL CONNECTION (WHILE USING ARDUINO)----------------------------------------
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

        while(true){

            try{
                double distance;

//---------------------------------------------------GET DISTANCE-------------------------------------------------------
                if(SIMULATION_MODE){

                    // Simulation sensor data (0 - 100)
                    distance = random.nextInt(100);

                    Thread.sleep(4000);

                }else{

                    String line = reader.readLine();

                    if(line == null || line.isEmpty()){
                        continue;
                    }

                    distance = Double.parseDouble(line.trim());

                }

//--------------------------------------------------DISPLAY DISTANCE--------------------------------------------------------------
                System.out.println("Distance: " + distance + " cm");

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

//--------------------------------------------------DATABASE STORAGE------------------------------------------------------
                 DatabaseManager.save(distance, status);
                // (Will store distance, status timestamp)

//--------------------------------------------------GRAPH GENERATION------------------------------------------------------
                // GraphGenerator.update(distance);
                // (Will update distance vs time graph)

//-----------------------------------------------------GUI UPDATE------------------------------------------------------
                // Dashboard.update(distance, status)
                // (Will update live dashboard)

                System.out.println("--------------------------------");
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}