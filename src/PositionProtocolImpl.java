
import peersim.core.Node;

/**
 * @author mokuhazushi
 */
public class PositionProtocolImpl implements PositionProtocol {
    private double posX, posY;
    private int maxSpeed, minSpeed;
    private int timePause;

    public PositionProtocolImpl(double posX, double posY, int maxSpeed, int minSpeed, int timePause) {
        this.posX = posX;
        this.posY = posY;
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
        this.timePause = timePause;
    }

    @Override
    public double getY() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getX() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMaxSpeed() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getMaxX() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getMaxY() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getTimePause() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public Object clone() {
        return new PositionProtocolImpl(posX, posY, maxSpeed, minSpeed, timePause);
    }

}
