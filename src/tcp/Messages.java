package tcp;

public class Messages {
    public static final String SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR\u0007\b";
    public static final String SERVER_LOGIN_FAILED = "300 LOGIN FAILED\u0007\b";
    public static final String SERVER_OK = "200 OK\u0007\b";
    public static final String SERVER_MOVE = "102 MOVE\u0007\b";
    public static final String SERVER_TURN_LEFT = "103 TURN LEFT\u0007\b";
    public static final String SERVER_TURN_RIGHT = "104 TURN RIGHT\u0007\b";
    public static final String SERVER_PICK_UP = "105 GET MESSAGE\u0007\b";
    public static final String SERVER_LOGOUT = "106 LOGOUT\u0007\b";
    public static final String SERVER_LOGIC_ERROR = "302 LOGIC ERROR\u0007\b";
    public static final String CLIENT_RECHARGING = "RECHARGING";
    public static final String CLIENT_FULL_POWER = "FULL POWER";

    public static final Integer SERVER_KEY = 54621;
    public static final Integer CLIENT_KEY = 45328;
}
