package tcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static tcp.Messages.*;

//Class handles communication with the robot
public class ClientHandler implements Runnable {
    Socket socket;
    BufferedReader in;
    PrintWriter out;
    Robot robot;

    public enum MessageType {
        CLIENT_CONFIRMATION,
        CLIENT_OK,
        CLIENT_USERNAME,
        CLIENT_MESSAGE
    };

    ClientHandler(Socket clientSocket) throws Exception {
        socket = clientSocket;
        InputStreamReader inputReader = new InputStreamReader(clientSocket.getInputStream());
        in = new BufferedReader(inputReader);
        out = new PrintWriter(clientSocket.getOutputStream());

        new Thread(this).start();
    }

    @Override
    public void run() {
        handle();
        close();
    }

    //Method reads data from buffer
    public String listen(int len, MessageType type) throws Exception {
        String str = "";
        int c, prevC = '\0';

        while ((c = in.read()) != -1) {
            socket.setSoTimeout(1000);
            str += (char) c;

            if((str.length() > 12 && type != MessageType.CLIENT_MESSAGE) //message isn't the secret message and is already longer than 12
               || str.length() > 100 //message is generally longer than 100
               || (str.length() > 11 && str.charAt(str.length() - 1) != '\b' && type != MessageType.CLIENT_MESSAGE) //message already has 12 chars but the last one isn't \b and it is not a secret message
               || (str.length() > 98 && str.charAt(str.length() - 1) != '\u0007')) { //secret message can't be ended properly without overstepping length

                System.out.println("Client message " + str + "(" + type + ")" + " is too long");
                out.write(SERVER_SYNTAX_ERROR);
                out.flush();
                close();
            }

            //End of sequence
            if (prevC == '\u0007' && c == '\b') {
                if(str.equals("RECHARGING\u0007\b")) {
                    socket.setSoTimeout(5000);
                    rechargeRobot();
                    System.out.println("Robot fully charged");
                    str = "";
                    continue;
                }
                else {
                    validateMessage(str, type);
                    if(str.length() > len) {
                        System.out.println("Client message " + str + "(" + type + ")" + " is too long");
                        out.write(SERVER_SYNTAX_ERROR);
                        out.flush();
                        close();
                    }
                    str = str.substring(0, str.length() - 2);
                    return str;
                }
            }

            prevC = c;
        }
        return "";
    }

    public void validateMessage(String msg, MessageType type) {
        if(type == MessageType.CLIENT_OK) {
            String[] array = msg.split(" ");

            if(!array[0].equals("OK") || array.length != 3 || array[1].contains(".") || array[2].contains(".")) {
                out.write(SERVER_SYNTAX_ERROR);
                out.flush();
                close();
                System.out.println("Syntax error for CLIENT_OK");
            }
        }
        else if(type == MessageType.CLIENT_CONFIRMATION) {
            if(msg.length() > 7 || msg.contains(" ")) {
                out.write(SERVER_SYNTAX_ERROR);
                out.flush();
                close();
                System.out.println("Syntax error for CLIENT_CONFIRMATION - too long");
            }
            try {
                Double num = Double.parseDouble(msg);
            } catch (NumberFormatException e) {
                out.write(SERVER_SYNTAX_ERROR);
                out.flush();
                close();
                System.out.println("Syntax error for CLIENT_CONFIRMATION - not a number");
            }
        }
    }

    public int getPasswd(String msg, Integer KEY) {
        int sum = 0;
        for(int i = 0; i < msg.length(); ++i) {
            sum += (int)msg.charAt(i);
        }

        return (((sum * 1000) % 65536) + KEY) % 65536;
    }

    //Method handles decoding and reacting to messages received from robot
    public void handle() {
        try {
            //authenticate robot
            authenticate();
            System.out.println("Robot " + robot.name + " authenticated");

            //initialize robot
            initialize();
            System.out.println("Robot " + robot.name + " initialized");

            //navigate robot to target square
            moveToSquare();

            //find secret message
            findMessage();

        } catch (Exception e) {
            System.out.println("Error handling client input: " + e);
        }
    }

    //Method handles authentication of robot
    public void authenticate() {
        System.out.println("Authenticating a new robot...");
        try {
            String login = listen(12, MessageType.CLIENT_USERNAME);
            Integer passwd = getPasswd(login, SERVER_KEY);

           // System.out.println("Login: " + login + ", passwd: " + passwd);
            out.write(passwd + "\u0007\b");
            out.flush();

            String clientConfirmation = listen(7, MessageType.CLIENT_CONFIRMATION);
            if(Integer.parseInt(clientConfirmation) != getPasswd(login, CLIENT_KEY)) {
                System.out.println("Login failed");
                out.write(SERVER_LOGIN_FAILED);
                out.flush();
                close();
            }

            robot = new Robot(login);
            out.write(SERVER_OK);
            out.flush();
        }
        catch (Exception e) {
            System.out.println("Authentication error: " + e);
        }
    }

    //Method figures out robot's initial coordinates and orientation
    public void initialize() {
        System.out.println("Initializing robot...");
        try {
            //figure out position
            forceMove(2);

            //figure out orientation
            robot.findOrientation();
        }
        catch(Exception e) {
            System.out.println("Initial move error: " + e);
        }
    }

    //Method navigates robot to target square ( [2,2], [2,-2], [-2,2], [-2,-2] ) -> to [-2,2]
    public void moveToSquare() {
        try {
            while (robot.currCoordinates.x != -2 || robot.currCoordinates.y != 2) {
                String move = robot.selectMove(new Coordinates(-2, 2));
                if(move.equals("")) continue;
                out.write(move);
                out.flush();
                String msg = listen(12, MessageType.CLIENT_OK);
                robot.setCoordinates(msg);
            }
        }
        catch(Exception e) {
            System.out.println("Navigation to target square failed: " + e);
        }
    }

    //Method leads robot to find the secret message hidden inside the target square
    public void findMessage() {
        try {
            for(int i = 0; i < 25; ++i) {

                //check current position for secret message
                out.write(SERVER_PICK_UP);
                out.flush();
                String msg = listen(100, MessageType.CLIENT_MESSAGE);

                //message found
                if(!msg.equals("")) {
                    out.write(SERVER_LOGOUT);
                    out.flush();
                    close();
                    break;
                }

                //adjust orientation while searching the target square
                if(i == 0 || i == 15 || i == 23) {
                    setOrientation(Robot.Orientation.RIGHT);
                }
                else if(i == 4 || i == 18) {
                    setOrientation(Robot.Orientation.DOWN);
                }
                else if(i == 8 || i == 20) {
                    setOrientation(Robot.Orientation.LEFT);
                }
                else if(i == 12 || i == 22) {
                    setOrientation(Robot.Orientation.UP);
                }

                forceMove(1);
            }
        }
        catch(Exception e) {
            System.out.println("Failed to find the secret message: " + e);
        }
    }

    public void setOrientation(Robot.Orientation or) throws Exception {
        while(robot.orientation != or) {
            robot.rotate();
            out.write(SERVER_TURN_RIGHT);
            out.flush();

            String ret = listen(12, MessageType.CLIENT_OK);
        }
    }

    public void forceMove(int moves) throws Exception {
        int cnt = 0;
        while (cnt < moves) {
            out.write(SERVER_MOVE);
            out.flush();
            String ret = listen(12, MessageType.CLIENT_OK);

            robot.setCoordinates(ret);
            if(robot.checkMoveSuccess()) {
                ++cnt;
            }
            else {
                System.out.println("Robot failed to move forward, try again");
            }
        }
    }

    //Method recharges robot
    public void rechargeRobot() throws Exception {
        System.out.println("Recharging robot...");
        String str = "";
        int c, prevC = '\0';

        while((c = in.read()) != -1) {
            str += (char) c;

            if (prevC == '\u0007' && c == '\b') {
                if(!str.equals("FULL POWER\u0007\b")) {
                    out.write(SERVER_LOGIC_ERROR);
                    out.flush();
                    close();
                    System.out.println("Robot hasn't been recharged yet, logic error");
                }
                else return;
            }

            prevC = c;
        }
    }


    public void close(){
        try {
            System.out.println("Closing connection");
            socket.close();
            out.close();
            in.close();
        }catch (Exception e){
            System.out.println("Can't close all resources: " + e);
        }
    }
}

