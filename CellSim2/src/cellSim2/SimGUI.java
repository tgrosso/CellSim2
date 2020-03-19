/**
	 * Copyright (C) 2016 Terri Applewhite-Grosso and Nancy Griffeth
	 * Package: cellSim2
	 * File: TestRunner.java
	 * Jul 14, 2016
	 *
	 *   Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
	 *
	 *   The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
	 *
	 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
	 * 
	 * 
	 */

package cellSim2;

/**
 * @author Terri Applewhite-Grosso
 *
 */
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.border.Border;

import java.util.Set;
import java.util.Iterator;

public class SimGUI {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final static int OPENING = 0, OPENSIM = 1, EDITSIM = 2;
	private final static int EDITPARAM=3, EDITCELL=4;
	private final static String basicFile = "BasicTestFile.txt";
	private SimGenerator simgen;
	private JFrame sf;
	private int state;
	private Border padding;
	private Font buttonFont, labelFont, inputFont, textFont, warningFont;
	private File baseDirectory;
	private JPanel statusPanel;
	private JLabel statusLabel;
	
	public SimGUI() {
		state = OPENING;
		sf = new SimFrame(this);
		padding = BorderFactory.createEmptyBorder(100, 20, 50, 100);
		buttonFont = new Font(Font.SANS_SERIF, Font.PLAIN, 20);
		labelFont = new Font(Font.SANS_SERIF, Font.BOLD, 20);
		inputFont = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
		textFont = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
		warningFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
		
		simgen = null;
		
		statusPanel = new JPanel();
		statusPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		statusLabel = new JLabel("Current Selected Directory: None");
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusPanel.add(statusLabel);
	}
	
	public void setBaseDirectory(File f){
		System.out.println(f.toString());
		if (f == null){
			statusLabel.setText("Current Selected Directory: None");
			return;
		}
		//TODO Do some checking that the user can write to this directory and it exists
		baseDirectory = f;
		while (!baseDirectory.isDirectory()){
			baseDirectory = baseDirectory.getParentFile();
		}
		statusLabel.setText("Current Selected Directory: " + baseDirectory);
		
		//If there is no basic file in this directory, make a blank basic file
		//Create a simGenerator with this directory as the output directory
		//TODO: should do something if there is no directory
	}
	
	public boolean isBaseDirectorySet(){
		return baseDirectory != null;
	}
	
	public File getBaseDirectory(){
		return baseDirectory;
	}
	
	private class SimFrame extends javax.swing.JFrame{
		JPanel cp;
		SimGUI parent;
		
		public SimFrame(SimGUI p){
			//FOR DEBUGGING!!
			//p.setBaseDirectory(new File("/Users⁩/terri/⁨Dropbox⁩/⁨FinalDissertationWork⁩/⁨ExperimentalData⁩/TestingGUI⁩"));
			parent = p;
			JFrame.setDefaultLookAndFeelDecorated(true);
			cp = new JPanel();
			setContentPane(cp);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			//setLayout(new BorderLayout());
			setTitle("Simulation Program");
			cp.setBackground(Color.black);
			
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			int screenWidth = (int)(dim.getWidth() *.75);
			int screenHeight = (int)(dim.getHeight() * .75);
			int xpos = (dim.width - screenWidth) / 2;
	        int ypos = (dim.height - screenHeight) / 2;
	        setPreferredSize(new Dimension(screenWidth, screenHeight));
	        setLocation(xpos, ypos);
	        cp.setLayout(new BorderLayout());
	        state = OPENING;
	        
	        cp.add(new mainPanel(this), BorderLayout.CENTER);
	        pack();
			validate();
			repaint();
			setVisible(true);
		}
		
		public void setState(int st){
			System.out.println("Setting state " + st);
			cp.removeAll();
			cp.setLayout(new BorderLayout());
			switch(st){
				case OPENING:	
								cp.add(new mainPanel(this), BorderLayout.CENTER);
								state = OPENING;
								break;
				case OPENSIM:	while(!parent.isBaseDirectorySet()){
									parent.setBaseDirectory(new DirectoryPicker(parent).getSelectedFile());
									if (parent.isBaseDirectorySet()){
										//System.out.println("SimGUI 143: " +parent.getBaseDirectory());
									}
									else{
										System.err.println("No directory set");
									}
								}
								cp.add(new SimPanel(this), BorderLayout.CENTER);
								state = OPENSIM;
								break;
				case EDITPARAM:	
								DefaultsPanel dp = new DefaultsPanel(this);
								dp.maxPanel();
								cp.add(dp, BorderLayout.CENTER);
								state = EDITPARAM;
								break;
				case EDITCELL:
								CellPanel cellp = new CellPanel(this);
								cellp.maxPanel();
								cp.add(cellp, BorderLayout.CENTER);
								state = EDITCELL;
								break;
				default:		
								cp.add(new mainPanel(this), BorderLayout.CENTER);
								state = OPENING;
								break;
			}
			cp.add(statusPanel, BorderLayout.PAGE_END);
			pack();
			validate();
			repaint();
			if (!isVisible()){
				setVisible(true);
			}
		}
	}
	
	private class mainPanel extends javax.swing.JPanel{
		SimFrame program;
		
		public mainPanel(SimFrame p){
			super();
			program = p;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBackground(Color.white);
			//setLayout(new GridLayout(4,1, 50, 50));
			
			add(Box.createVerticalGlue());
			
			JButton openSim = new JButton("Edit or Create Simulation");
			openSim.setAlignmentX(Component.CENTER_ALIGNMENT);
			openSim.setFont(buttonFont);
			openSim.setMargin(new Insets(20, 100, 20, 100));
			openSim.addActionListener( new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	System.out.println("Start new simulation");
	                p.setState(OPENSIM);
	            }
	        });
			add(openSim);
			
			add(Box.createVerticalGlue());
			
			JButton runSim = new JButton("Run Existing Simulation");
			runSim.setAlignmentX(Component.CENTER_ALIGNMENT);
			runSim.setFont(buttonFont);
			runSim.setMargin(new Insets(20, 100, 20, 100));
			add(runSim);
			
			add(Box.createVerticalGlue());
			JButton seqSim = new JButton("Create Simulation Sequence");
			seqSim.setAlignmentX(Component.CENTER_ALIGNMENT);
			seqSim.setFont(buttonFont);
			seqSim.setMargin(new Insets(20, 100, 20, 100));
			add(seqSim);
			
			add(Box.createVerticalGlue());
			
			JButton quit = new JButton("Quit Program");
			quit.setAlignmentX(Component.CENTER_ALIGNMENT);
			quit.setFont(buttonFont);
			quit.setMargin(new Insets(20, 100, 20, 100));
			quit.addActionListener( new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	System.out.println("Quit Program");
	                program.removeAll();
	                System.exit(0);
	            }
	        });
			add(quit);
			
			add(Box.createVerticalGlue());
		}
	}
	
	private class SimPanel extends javax.swing.JPanel{
		SimFrame program;
		
		public SimPanel(SimFrame p){
			program = p;
			setLayout(new BorderLayout());
			setBackground(Color.white);

			
			DefaultsPanel defaultVals = new DefaultsPanel(program);
			JPanel otherVals = new JPanel();
			JPanel complete = new JPanel();
			
			otherVals.setLayout(new BoxLayout(otherVals, BoxLayout.LINE_AXIS));
			otherVals.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			otherVals.add(Box.createHorizontalGlue());
			otherVals.add(new ProteinPanel(p));
			otherVals.add(Box.createHorizontalGlue());
			otherVals.add(Box.createRigidArea(new Dimension(10, 0)));
			otherVals.add(new CellPanel(p));
			otherVals.add(Box.createHorizontalGlue());
			otherVals.add(Box.createRigidArea(new Dimension(10, 0)));
			otherVals.add(new GradientPanel(p));
			otherVals.add(Box.createHorizontalGlue());
			otherVals.add(Box.createRigidArea(new Dimension(10, 0)));
			otherVals.add(new WallPanel(p));
			otherVals.add(Box.createHorizontalGlue());
			
			complete.setLayout(new FlowLayout(FlowLayout.CENTER));
			JButton done = new JButton("Done");
			done.setAlignmentX(Component.CENTER_ALIGNMENT);
			done.setFont(buttonFont);
			done.setMargin(new Insets(20, 40, 20, 40));
			done.addActionListener( new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	System.out.println("Finish Editing Simulation");
	                p.setState(OPENING);
	            }
	        });
			complete.add(done);
			
			add(defaultVals, BorderLayout.PAGE_START);
			defaultVals.minPanel();
			add(otherVals, BorderLayout.CENTER);
			add(complete, BorderLayout.PAGE_END);
			
			
		}
	}
	
	private class DefaultsPanel extends javax.swing.JPanel{
		SimFrame program; 
		boolean display;
		
		public DefaultsPanel(SimFrame p){
			program = p;
			display = false;
			minPanel();
		}
		
		public void minPanel(){
			removeAll();
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			JLabel simTitle = new JLabel("Simulation Parameters");
			simTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
			simTitle.setFont(labelFont);
			add(simTitle);
			add(Box.createRigidArea(new Dimension(0, 10)));
			
			JButton editDefs = new JButton("Edit Parameters");
			editDefs.setAlignmentX(Component.CENTER_ALIGNMENT);
			editDefs.setFont(buttonFont);
			editDefs.setMargin(new Insets(20, 30, 20, 30));
			editDefs.addActionListener( new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	System.out.println("Open Edit Parameters");
	                program.setState(EDITPARAM);;
	            }
	        });
			add(editDefs);
			
			revalidate();
			repaint();
		}
		
		public void maxPanel(){
			removeAll();
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
			JLabel simTitle = new JLabel("Simulation Parameters");
			simTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
			simTitle.setFont(labelFont);
			add(simTitle);
			add(Box.createRigidArea(new Dimension(0, 10)));
			add(Box.createVerticalGlue());
			
			if (baseDirectory == null){
				JLabel noDirectory = new JLabel("You need to set the base directory to continue.");
				noDirectory.setAlignmentX(Component.CENTER_ALIGNMENT);
				noDirectory.setFont(labelFont);
				add(noDirectory);

				JButton cancelDefs = new JButton("Cancel");
				cancelDefs.setAlignmentX(Component.CENTER_ALIGNMENT);
				cancelDefs.setFont(buttonFont);
				cancelDefs.setMargin(new Insets(20, 30, 20, 30));
				cancelDefs.addActionListener( new ActionListener() {
		            public void actionPerformed(ActionEvent event) {
		            	System.out.println("Cancel Need Base Directory");
		            	//Do NOT SAVE!
		                program.setState(OPENSIM);;
		            }
		        });
				add(cancelDefs);
			}
			
			else{ 
				if (simgen == null){
					File defFile = new File(baseDirectory,basicFile);
					try{
						defFile.createNewFile();
						//Will create an empty basic file if one does not exist
					}
					catch(IOException e){
						System.err.println("Cannot create a file in this directory.");
					}
					if (defFile.exists()){	
						simgen = new SimGenerator(defFile, baseDirectory);
					}
					else{
						//The basic file is not there
						System.err.println("There is a major problem writing to this directory.");
						System.err.println("Quitting...");
						System.exit(2);
					}
				}
				
				Defaults defaults = simgen.getValues();
				String[] paramList = defaults.getParamList("Param");
			
				for (int i = 0; i < paramList.length; i++){
					if (paramList[i] == ""){
						//Just add an empty line
						add(Box.createRigidArea(new Dimension(0, 10)));
						continue;
					}
					JPanel p = new JPanel();
					p.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 5));
				
					String key = paramList[i];
					try{
						String value = defaults.getValue("", key);
						String tit = defaults.getStringTitle(key);
						//System.out.println(key + " " + tit + " "+ value);
						JLabel text = new JLabel(tit);
						text.setHorizontalAlignment(JLabel.CENTER);
						text.setFont(textFont);
						p.add(text);
						JTextField inp = new JTextField(value, 10);
						inp.setFont(inputFont);
						inp.setHorizontalAlignment(JTextField.RIGHT);
						p.add(inp);
						add(p);
					}
					catch(SimException e){
						System.err.println("Cannot get value for key " + key);
					}
				}
				add(Box.createVerticalGlue());
			}
			//Now put in the Cancel and Close buttons
			JPanel closeCancel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton cancelDefs = new JButton("Cancel");
			cancelDefs.setAlignmentX(Component.CENTER_ALIGNMENT);
			cancelDefs.setFont(buttonFont);
			cancelDefs.setMargin(new Insets(20, 30, 20, 30));
			cancelDefs.addActionListener( new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	System.out.println("Cancel Edit Parameters");
	            	//Do NOT SAVE!
	                program.setState(OPENSIM);;
	            }
	        });
			closeCancel.add(cancelDefs);
			
			if (simgen != null){
				JButton closeDefs = new JButton("Save Parameters");
				closeDefs.setAlignmentX(Component.CENTER_ALIGNMENT);
				closeDefs.setFont(buttonFont);
				closeDefs.setMargin(new Insets(20, 30, 20, 30));
				closeDefs.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						System.out.println("Save Edit Parameters");
						//TODO Do the saving / editing here
						//Need to iterate through the component panels
						DefaultsPanel dp = (DefaultsPanel)(closeDefs.getParent().getParent());
						dp.saveValues();
						program.setState(OPENSIM);;
					}
				});
				closeCancel.add(closeDefs);
				add(closeCancel);
			}
			revalidate();
			repaint();
		}
		public void saveValues(){
			System.out.println("Saving the default values");
			Defaults def = simgen.getValues();
			//Get the values from the page
			for (Component c : this.getComponents()){
				if (c instanceof JPanel) {
			        //This should be a panel with a label and a text field
					JPanel pan = (JPanel)c;
					Component[] panList = pan.getComponents();
					if (panList[0] instanceof JLabel && panList[1] instanceof JTextField){
						//System.out.println("This is a default value");
						JLabel lab = (JLabel)panList[0];
						String key = lab.getText();
						JTextField field = (JTextField)panList[1];
						//System.out.println(key + ", " + field.getText());
						String[] valList = new String[1];
						valList[0] = field.getText();
						def.addCurrent(key, valList);
					}
				}
			}
			//Write out the SimGenerator
			simgen.outputFiles(baseDirectory);
		}
	}

	
	private class CellPanel extends javax.swing.JPanel{
		SimFrame program;
		
		public CellPanel(SimFrame p){
			program = p;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			//setBackground(Color.red);
			minPanel();
		}
		
		private void minPanel(){
			removeAll();
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			JLabel cellLabel = new JLabel("Cell Types");
			cellLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			cellLabel.setFont(labelFont);
			add(cellLabel);
			
			JButton newCell = new JButton("Edit Cells");
			newCell.setAlignmentX(Component.CENTER_ALIGNMENT);
			newCell.setFont(buttonFont);
			newCell.setMargin(new Insets(20, 30, 20, 30));
			newCell.addActionListener( new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	System.out.println("Open Cell Editing");
	                program.setState(EDITCELL);;
	            }
	        });
			add(newCell);
			
			revalidate();
			repaint();
		}
		
		public void maxPanel(){
			removeAll();
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
			JLabel simTitle = new JLabel("Cell Types");
			simTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
			simTitle.setFont(labelFont);
			add(simTitle);
			add(Box.createRigidArea(new Dimension(0, 10)));
			add(Box.createVerticalGlue());
			
			if (baseDirectory == null){
				JLabel noDirectory = new JLabel("You need to set the base directory to continue.");
				noDirectory.setAlignmentX(Component.CENTER_ALIGNMENT);
				noDirectory.setFont(labelFont);
				add(noDirectory);
			}
			
			else{ 
				if (simgen == null){
					File defFile = new File(baseDirectory,basicFile);
					try{
						defFile.createNewFile();
						//Will create an empty basic file if one does not exist
					}
					catch(IOException e){
						System.err.println("Cannot create a file in this directory.");
					}
					if (defFile.exists()){	
						simgen = new SimGenerator(defFile, baseDirectory);
					}
					else{
						//The basic file is not there
						System.err.println("There is a major problem writing to this directory.");
						System.err.println("Quitting...");
						System.exit(2);
					}
				}
				
				JLabel curr = new JLabel("Current Cells");
				curr.setAlignmentX(Component.CENTER_ALIGNMENT);
				curr.setFont(labelFont);
				add(curr);
				add(Box.createRigidArea(new Dimension(0, 10)));
				add(Box.createVerticalGlue());
				
				Defaults defaults = simgen.getValues();
				
				HashMap<String, String> cellData = defaults.getCells();
				for (String type: cellData.keySet()){
					JPanel p = new JPanel();
					JLabel n = new JLabel(type);
					n.setAlignmentX(Component.CENTER_ALIGNMENT);
					n.setFont(labelFont);
					p.add(n);
					JButton ed = new JButton("Edit");
					ed.setFont(buttonFont);
					p.add(ed);
					JButton rem = new JButton("Remove");
					rem.setFont(buttonFont);
					p.add(rem);
					add(p);
				}
				add(Box.createVerticalGlue());
				
				String[] paramList = defaults.getParamList("Cell");
				JLabel nCell = new JLabel("Add A Cell Type");
				nCell.setAlignmentX(Component.CENTER_ALIGNMENT);
				nCell.setFont(labelFont);
				add(nCell);
				String outputString = "";
				
				JPanel topPanel = new JPanel(new GridLayout(1,2));
				JPanel leftPanel = new JPanel();
				leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
				leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
				leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
				JPanel rightPanel = new JPanel();
				rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
				
				JButton addNew = new JButton("Add New Cell");
				addNew.setAlignmentX(Component.CENTER_ALIGNMENT);
				addNew.setFont(buttonFont);
				leftPanel.add(addNew);
				leftPanel.add(Box.createVerticalGlue());
				
				for (int i = 0; i < paramList.length; i++){
					if (paramList[i] == ""){
						//Just add an empty line
						rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
						continue;
					}
					JPanel p = new JPanel();
					p.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 5));
				
					String key = paramList[i];

					String value = Defaults.getSpecialDefault("Cell", key);
					String tit = defaults.getStringTitle(key);
					//System.out.println(key + " " + tit + " "+ value);
					JLabel text = new JLabel(tit);
					text.setHorizontalAlignment(JLabel.CENTER);
					text.setFont(textFont);
					p.add(text);
					JTextField inp = new JTextField(value, 10);
					inp.setFont(inputFont);
					inp.setHorizontalAlignment(JTextField.RIGHT);
					p.add(inp);
					rightPanel.add(p);
					if (key.equals("Name")){
						JLabel t = new JLabel("(No spaces or special characters)");
						t.setFont(warningFont);
						rightPanel.add(t);
					}
					
				}
				//Add the option to add in a receptor
				topPanel.add(leftPanel);
				topPanel.add(rightPanel);
				add(topPanel);
				add(Box.createRigidArea(new Dimension(0, 10)));
				add(Box.createVerticalGlue());
			}
			//Now put in the Cancel/Update button
			JPanel closeCancel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton cancelDefs = new JButton("Cancel");
			cancelDefs.setAlignmentX(Component.CENTER_ALIGNMENT);
			cancelDefs.setFont(buttonFont);
			cancelDefs.setMargin(new Insets(20, 30, 20, 30));
			cancelDefs.addActionListener( new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	System.out.println("Done Editing Cells");
	            	//Do NOT SAVE!
	                program.setState(OPENSIM);;
	            }
	        });
			closeCancel.add(cancelDefs);
			if (simgen != null){
				JButton closeDefs = new JButton("Update Changes");
				closeDefs.setAlignmentX(Component.CENTER_ALIGNMENT);
				closeDefs.setFont(buttonFont);
				closeDefs.setMargin(new Insets(20, 30, 20, 30));
				closeDefs.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						System.out.println("Save Cell Parameters");
						//TODO Do the saving / editing here
						//Need to iterate through the component panels
						//DefaultsPanel dp = (DefaultsPanel)(closeDefs.getParent().getParent());
						//dp.saveValues();
						program.setState(OPENSIM);;
					}
				});
				closeCancel.add(closeDefs);
			}
			
			add(closeCancel);
			
			revalidate();
			repaint();
		}
		
		public void removeCellType(String type){
			
		}
		
		public void updateCellData(String[] updates){
			
		}
		
		public void addNewCellType(String name, String[] data){
			
		}
	}
	
	private class ProteinPanel extends javax.swing.JPanel{
		SimFrame program;
		
		public ProteinPanel(SimFrame p){
			program = p;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			//setBackground(Color.green);
			minPanel();
		}
		
		private void minPanel(){
			removeAll();
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			JLabel proTitle = new JLabel("Proteins");
			proTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
			proTitle.setFont(labelFont);
			add(proTitle);
			
			JButton newPro = new JButton("Edit Proteins");
			newPro.setAlignmentX(Component.CENTER_ALIGNMENT);
			newPro.setFont(buttonFont);
			newPro.setMargin(new Insets(20, 30, 20, 30));
			add(newPro);
			
			revalidate();
			repaint();
		}
	}
	
	private class GradientPanel extends javax.swing.JPanel{
		SimFrame program;
		
		public GradientPanel(SimFrame p){
			program = p;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			//setBackground(Color.yellow);
			minPanel();
		}
		
		private void minPanel(){
			removeAll();
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			JLabel gradTitle = new JLabel("Gradients");
			gradTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
			gradTitle.setFont(labelFont);
			add(gradTitle);
			
			JButton newGrad = new JButton("Edit Gradients");
			newGrad.setAlignmentX(Component.CENTER_ALIGNMENT);
			newGrad.setFont(buttonFont);
			newGrad.setMargin(new Insets(20, 30, 20, 30));
			add(newGrad);
			
			revalidate();
			repaint();
		}
	}
	
	private class WallPanel extends javax.swing.JPanel{
		SimFrame program;
		
		public WallPanel(SimFrame p){
			program = p;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			//setBackground(Color.yellow);
			minPanel();
		}
		
		private void minPanel(){
			removeAll();
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			JLabel gradTitle = new JLabel("Walls and Vessels");
			gradTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
			gradTitle.setFont(labelFont);
			add(gradTitle);
			
			JButton newGrad = new JButton("Add New Wall/Vessel");
			newGrad.setAlignmentX(Component.CENTER_ALIGNMENT);
			newGrad.setFont(buttonFont);
			newGrad.setMargin(new Insets(20, 30, 20, 30));
			add(newGrad);
			
			revalidate();
			repaint();
		}
	}
		
	private class DirectoryPicker extends javax.swing.JFileChooser{
		//if baseDirectory == null, set the initial value to the current directory
		SimGUI parent;
		
		public DirectoryPicker(SimGUI p){
			parent = p;
			
			if (!parent.isBaseDirectorySet()){
				setCurrentDirectory(parent.getBaseDirectory());
			}
			else{
				setCurrentDirectory(new File("."));
			}
			setDialogTitle("Choose Directory of Simulation");
			
			setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			setAcceptAllFileFilterUsed(false);
			setMultiSelectionEnabled(false);
			showSaveDialog(parent.sf);
		}
		
	}
	
	public static void main(String[] args){
		try {
            // Set cross-platform Java L&F (also called "Metal")
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} 
		catch (UnsupportedLookAndFeelException e) {
			// handle exception
		}
		catch (ClassNotFoundException e) {
			// handle exception
		}
		catch (InstantiationException e) {
			// handle exception
		}
		catch (IllegalAccessException e) {
			// handle exception
		}
        
		SimGUI s = new SimGUI();
	}

}
