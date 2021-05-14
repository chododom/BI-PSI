package tcp;

import java.util.Objects;

public class Coordinates {
    public Integer x;
    public Integer y;

    Coordinates() {
        x = -111;
        y = -111;
    }

    Coordinates(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        Coordinates that = (Coordinates) o;
        return x.equals(that.x) && y.equals(that.y);
    }
}