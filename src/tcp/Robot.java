package tcp;

import static tcp.Messages.*;

public class Robot {
    String name;
    Coordinates prevCoordinates;
    Coordinates currCoordinates;
    Orientation orientation;

    public enum Orientation {
        UP,
        RIGHT,
        DOWN,
        LEFT
    };

    Robot(String newName) {
        name = newName;
        prevCoordinates = new Coordinates();
        currCoordinates = new Coordinates();
        orientation = Orientation.UP;
    }

    public void rotate() {
        switch(orientation) {
            case DOWN:
                orientation = Orientation.LEFT;
                break;
            case LEFT:
                orientation = Orientation.UP;
                break;
            case UP:
                orientation = Orientation.RIGHT;
                break;
            case RIGHT:
                orientation = Orientation.DOWN;
                break;
            default:
                break;
        }
    }

    //Method parses coordinates from a message received rom the robot and saves them
    public void setCoordinates(String msg) {
        String[] array = msg.split(" ");

        prevCoordinates.x = currCoordinates.x;
        prevCoordinates.y = currCoordinates.y;

        currCoordinates.x = Integer.parseInt(array[1]);
        currCoordinates.y = Integer.parseInt(array[2]);
    }

    //Method checks if robot has moved
    public Boolean checkMoveSuccess() {
        return !prevCoordinates.equals(currCoordinates);
    }

    //Method figures out which way the robot is oriented
    public void findOrientation() {
        if(currCoordinates.x > prevCoordinates.x) {
            orientation = Orientation.RIGHT;
        }
        else if(currCoordinates.x < prevCoordinates.x) {
            orientation = Orientation.LEFT;
        }
        else if(currCoordinates.y > prevCoordinates.y) {
            orientation = Orientation.UP;
        }
        else if(currCoordinates.y < prevCoordinates.y) {
            orientation = Orientation.DOWN;
        }
    }

    //Method figures out the best move to get closer to target
    public String selectMove(Coordinates target) {
        if(currCoordinates.y > target.y) {
            if(orientation != Orientation.DOWN) {
                rotate();
                return SERVER_TURN_RIGHT;
            }
            return SERVER_MOVE;
        }
        else if(currCoordinates.y < target.y) {
            if(orientation != Orientation.UP) {
                rotate();
                return SERVER_TURN_RIGHT;
            }
            return SERVER_MOVE;
        }

        if(currCoordinates.x > target.x) {
            if(orientation != Orientation.LEFT) {
                rotate();
                return SERVER_TURN_RIGHT;
            }
            return SERVER_MOVE;
        }
        else if(currCoordinates.x < target.x) {
            if(orientation != Orientation.RIGHT) {
                rotate();
                return SERVER_TURN_RIGHT;
            }
            return SERVER_MOVE;
        }

        return "";
    }
}
