import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class PathGenerator extends JFrame implements KeyListener, MouseListener {

	private BufferedImage background = ImageIO.read(new File("Field.png"));
	private BufferedImage bufferImage = new BufferedImage(1080, 540, BufferedImage.TYPE_INT_RGB);
	private ArrayList<double[]> path = new ArrayList<>();
	private Graphics buffer = bufferImage.getGraphics();
	private Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

	private final String address;
	private double deltaTime;

	private enum Mode {
		path,
		heading
	}

	private int headingIndex = 0;

	private Mode mode = Mode.path;

	public static void main(String args[]) {
		try {
			PathGenerator pg = new PathGenerator();
			pg.paint(pg.getGraphics());
			while (true) {
				pg.repaint();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public PathGenerator() throws IOException {
		this.setSize(1080, 540);
		this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.addMouseListener(this);
		this.addKeyListener(this);
		this.setVisible(true);

		String s = JOptionPane.showInputDialog("Address of the path requester");
		if(s != null)
			address = s;
		else
			address = "UNSPECIFIED";
		s = JOptionPane.showInputDialog("time between setpoints in the profile, in seconds.");
		if(s != null){
			try{
				deltaTime = Double.parseDouble(s);
			}catch(NumberFormatException e){
				deltaTime = .05;
			}
		} else
			deltaTime = .05;
	}

	private void saveMaptoYAML() throws IOException {
		String file;
		try {
			file = JOptionPane.showInputDialog("File name") + ".yml";
		}catch(IndexOutOfBoundsException e){
			e.printStackTrace();
			file = "AutoPath.yml";
			System.out.println("Continuing save, default file " + file);
		}
		PrintWriter output = new PrintWriter(new File(file));
		String fileHeading = String.format(
				"autoStartupCommand:\n" +
						"    org.usfirst.frc.team449.robot.commands.general.GoToPositionSequence:\n" +
						"        '@id': simpleSwitchStartRightLeftSwitch\n" +
						"        poseEstimator:\n" +
						"           org.usfirst.frc.team449.robot.other.UnidirectionalPoseEstimator:\n" +
						"                poseEstimator\n" +
						"        pathRequester:\n" +
						"           org.usfirst.frc.team449.robot.components.PathRequester:\n" +
						"                '@id': autoPath\n" +
						"                address: %s\n" +
						"        subsystem:\n" +
						"            org.usfirst.frc.team449.robot.drive.unidirectional.DriveUnidirectionalWithGyroShiftable:\n" +
						"                drive\n" +
						"        deltaTime: %f\n" +
						"        path:\n", address, deltaTime);
		output.print(fileHeading);
		for(double[] node : path){
			//Normalize values to fit with the field
			node[2] = -node[2] * 180 / Math.PI ;
			node[2] = node[2] + ((node[2] <= -180) ? 360 : 0);
			node[0] = node[0] / this.getWidth() * 54;
			node[1] = node[1] / this.getHeight() * 27;
			output.printf("            - {%f, %f, %f}\n", node[0], node[1], node[2]);
		}
		output.close();
		try {
			JOptionPane.showMessageDialog(this, "Finished");
		}catch(IndexOutOfBoundsException e){
			e.printStackTrace();
		}
		System.out.println("Finished save");
		System.exit(0);
	}

	@Override
	public void paint(Graphics g) {
		buffer.drawImage(background, 0, 0, 1080, 540, null);
		if (path.size() >= 2) {
			buffer.setColor(Color.RED);
			double[] last = path.get(0);
			for (int i = 1; i < path.size(); i++) {
				buffer.drawLine((int) last[0], (int) last[1], (int) path.get(i)[0], (int) path.get(i)[1]);
				last = path.get(i);
			}
		}

		if (mode == Mode.heading) {
			buffer.setColor(Color.blue);
			buffer.fillOval((int) path.get(headingIndex)[0] - 4, (int) path.get(headingIndex)[1] - 4, 8, 8);
			buffer.drawLine((int) path.get(headingIndex)[0],
					(int) path.get(headingIndex)[1], (int) path.get(headingIndex)[0] + (int) (30 * Math.cos(path.get(headingIndex)[2])), (int) path.get(headingIndex)[1] + (int) (30 * Math.sin(path.get(headingIndex)[2])));

		}

		buffer.setFont(new Font("TimesRoman", 1,20));
		switch(mode){
			case heading:
				buffer.setColor(Color.BLUE);
				buffer.drawString("Shift: Last Point",10,50);
				buffer.drawString("Enter: Next Point",10,70);
				buffer.drawString("S: Save",10,90);
				break;
			case path:
				buffer.setColor(Color.BLUE);
				buffer.drawString("Left Click: New Point",10,50);
				buffer.drawString("U: Undo Last Placement",10,70);
				buffer.drawString("S: Move on to Heading Assignment",10,90);
				break;
			default:
				break;
		}


		g.drawImage(bufferImage, 0, 0, null);

	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_U && mode == Mode.path && path.size() > 0) {
			path.remove(path.size() - 1);
		}else
		if (e.getKeyCode() == KeyEvent.VK_S) {
			if(mode == Mode.path) {
				int answer;
				try {
					answer = JOptionPane.showConfirmDialog(this, "Move onto headings?");
				}catch(IndexOutOfBoundsException ev){
					ev.printStackTrace();
					answer = 1;
				}
				if (answer == 0)
					if(path.size() > 0)
						mode = Mode.heading;
					else JOptionPane.showMessageDialog(this,"No path enetered");

			}
			else if(mode == Mode.heading){
				int answer;
				try {
					answer = JOptionPane.showConfirmDialog(this, "Save Final Map?");
				}catch(IndexOutOfBoundsException ev){
					ev.printStackTrace();
					answer = 1;
				}
				if (answer == 0) {
					try {
						saveMaptoYAML();
					} catch (IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(this, "Error | IOException in Saving Map");
					}
				}
			}
		}else
		if (e.getKeyCode() == KeyEvent.VK_ENTER && mode == Mode.heading && headingIndex < path.size() - 1) {
			headingIndex++;
		}else
		if (e.getKeyCode() == KeyEvent.VK_SHIFT && mode == Mode.heading && headingIndex > 0) {
			headingIndex--;
		}

	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (mode == Mode.path) {
			path.add(new double[]{e.getX(), e.getY(), 0});
		}else if (mode == Mode.heading) {
			double angle = 0;
			angle = Math.atan((path.get(headingIndex)[1] - e.getY()) / (path.get(headingIndex)[0] - e.getX()));
			if (path.get(headingIndex)[0] - e.getX() > 0)
				angle -= Math.PI;
			path.get(headingIndex)[2] = angle;
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}