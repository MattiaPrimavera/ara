
import javax.swing.JPanel;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Node;

/**
 * @author mokuhazushi
 */
public class PositionProtocolImpl implements PositionProtocol, CDProtocol {
    private static final String PAR_MAXX = "maxX";
    private static final String PAR_MAXY = "maxY";
    private static final String PAR_MAXSPEED = "maxSpeed";
    private static final String PAR_TIMEPAUSE = "timePause";
    
    private double posX, posY, maxX, maxY;
    private int maxSpeed, minSpeed, curSpeed;
    private int timePause;
    private JPanel monitor;

    public PositionProtocolImpl(double posX, double posY, int maxSpeed, int minSpeed, int timePause) {
        this.posX = posX;
        this.posY = posY;
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
        this.timePause = timePause;
    }
    
    public PositionProtocolImpl(String prefix) {
        this.maxX=Configuration.getDouble(prefix+"."+PAR_MAXX);
        this.maxY=Configuration.getDouble(prefix+"."+PAR_MAXY);
        this.maxSpeed=Configuration.getInt(prefix+"."+PAR_MAXSPEED);
        this.timePause=Configuration.getInt(prefix+"."+PAR_TIMEPAUSE);
        this.posX = Math.random()*maxX;
        this.posY = Math.random()*maxY;
        this.minSpeed = 1;
        this.curSpeed = (int)(Math.random()*(maxSpeed-minSpeed) + minSpeed);
    }
    
    public PositionProtocolImpl(double maxX, double maxY, int maxSpeed, int timePause) {
        this.maxX = maxX;
        this.maxY = maxY;
        this.posX = Math.random()*maxX;
        this.posY = Math.random()*maxY;
        this.maxSpeed = maxSpeed;
        this.minSpeed = 1;
        this.timePause = timePause;
        this.curSpeed = (int)(Math.random()*(maxSpeed-minSpeed) + minSpeed);
    }

    @Override
    public double getY() {
        return posY;
    }

    @Override
    public double getX() {
        return posX;
    }

    @Override
    public int getMaxSpeed() {
        return maxSpeed;
    }

    @Override
    public double getMaxX() {
        return maxX;
    }

    @Override
    public double getMaxY() {
        return maxY;
    }

    @Override
    public int getTimePause() {
        return timePause;
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {

    }
    
    @Override
    public Object clone() {
        return new PositionProtocolImpl(maxX, maxY, maxSpeed, timePause);
    }
    
    public void setMonitor(JPanel panel) {
        this.monitor = panel;
    }

    @Override
    public void nextCycle(Node node, int protocolID) {
        //RANDOMIZE SPEED
        this.curSpeed = (int)(Math.random()*(maxSpeed-minSpeed) + minSpeed);
        
        //MOVE NODE
        if ((int)(Math.random()*100) < 50) { //50% chance to change direction
            int direction = (int)(Math.random()*4);
            switch(direction) {
                case 0:
                    //GO UP
                    if (posY - curSpeed < 0)
                        posY += curSpeed;
                    else
                        posY -= curSpeed;
                    break;
                case 1:
                    //GO DOWN
                    if (posY + curSpeed > maxY)
                        posY -= curSpeed;
                    else
                        posY += curSpeed;
                    break;
                case 2:
                    //GO LEFT
                    if (posX - curSpeed < 0)
                        posX += curSpeed;
                    else
                        posX -= curSpeed;
                    break;
                case 3:
                    //GO RIGHT
                    if (posX + curSpeed > maxX)
                        posX -= curSpeed;
                    else
                        posX += curSpeed;
                    break;
            }
        }
        
        //REPAINT
        monitor.repaint();
        try {
            Thread.sleep(timePause);
        }
        catch(InterruptedException e) {}
    }
    
    

}
