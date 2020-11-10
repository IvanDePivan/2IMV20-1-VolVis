package gui;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.jogamp.opengl.awt.GLJPanel;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import volume.Volume;
import volvis.RaycastMode;
import volvis.RaycastRenderer;
import volvis.Visualization;

/**
 *
 * @author michel
 */
public class VolVisApplication extends JFrame {

    Visualization visualization;
    Volume volume;
    RaycastRenderer raycastRenderer;

    /**
     * Creates new form VolVisApplication
     */
    public VolVisApplication() {
        initComponents();
        this.setTitle("2IMV20 Volume Visualization");

        //GLCanvas glPanel = new GLCanvas();
        GLJPanel glPanel = new GLJPanel();
        renderPanel.setLayout(new BorderLayout());
        renderPanel.add(glPanel, BorderLayout.CENTER);
        // Create a new visualization for the OpenGL panel
        visualization = new Visualization(glPanel);
        glPanel.addGLEventListener(visualization);

        raycastRenderer = new RaycastRenderer();
        visualization.addRenderer(raycastRenderer);
        raycastRenderer.addTFChangeListener(visualization);
        tabbedPanel.addTab("Raycaster", raycastRenderer.getPanel());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        splitPane = new JSplitPane();
        tabbedPanel = new JTabbedPane();
        loadVolume = new JPanel();
        loadButton = new JButton();
        jScrollPane1 = new JScrollPane();
        infoTextPane = new JTextPane();
        renderPanel = new JPanel();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        splitPane.setDividerLocation(600);

        loadButton.setText("Load volume");
        loadButton.addActionListener(this::loadButtonActionPerformed);

        infoTextPane.setEditable(false);
        jScrollPane1.setViewportView(infoTextPane);

        GroupLayout loadVolumeLayout = new GroupLayout(loadVolume);
        loadVolume.setLayout(loadVolumeLayout);
        loadVolumeLayout.setHorizontalGroup(
            loadVolumeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(loadVolumeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(loadVolumeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(loadVolumeLayout.createSequentialGroup()
                        .addComponent(loadButton)
                        .addGap(0, 308, Short.MAX_VALUE)))
                .addContainerGap())
        );
        loadVolumeLayout.setVerticalGroup(
            loadVolumeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(loadVolumeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(loadButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabbedPanel.addTab("Load", loadVolume);

        splitPane.setRightComponent(tabbedPanel);

        GroupLayout renderPanelLayout = new GroupLayout(renderPanel);
        renderPanel.setLayout(renderPanelLayout);
        renderPanelLayout.setHorizontalGroup(
            renderPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 598, Short.MAX_VALUE)
        );
        renderPanelLayout.setVerticalGroup(
            renderPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 619, Short.MAX_VALUE)
        );

        splitPane.setLeftComponent(renderPanel);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(splitPane)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(splitPane)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void loadButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_loadButtonActionPerformed
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                if (f.isFile()) {
                    if (f.getName().toLowerCase().endsWith(".fld")) {
                        return true;
                    }
                }
                return f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "AVS files";
            }
        });

        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            volume = new Volume(file);

            String infoText = "Volume data info:\n";
            infoText = infoText.concat(file.getName() + "\n");
            infoText = infoText.concat("dimensions:\t\t" + volume.getDimX() + " x " + volume.getDimY() + " x " + volume.getDimZ() + "\n");
            infoText = infoText.concat("voxel value range:\t" + volume.getMinimum() + " - " + volume.getMaximum());
            infoTextPane.setText(infoText);
            tabbedPanel.remove(raycastRenderer.getTFPanel());
            tabbedPanel.remove(raycastRenderer.getTF2DPanel());
            tabbedPanel.remove(raycastRenderer.getTFPanelBack());
            tabbedPanel.remove(raycastRenderer.getTF2DPanelBack());
            raycastRenderer.setVolume(volume);
            tabbedPanel.addTab("Front Transfer Function", raycastRenderer.getTFPanel());
            tabbedPanel.addTab("Front 2D Transfer Function", raycastRenderer.getTF2DPanel());
            tabbedPanel.addTab("Back Transfer Function", raycastRenderer.getTFPanelBack());
            tabbedPanel.addTab("Back 2D Transfer Function", raycastRenderer.getTF2DPanelBack());
            
            visualization.update();
        }
    }//GEN-LAST:event_loadButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(VolVisApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        EventQueue.invokeLater(() -> new VolVisApplication().setVisible(true));
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JTextPane infoTextPane;
    private JScrollPane jScrollPane1;
    private JButton loadButton;
    private JPanel loadVolume;
    private JPanel renderPanel;
    private JSplitPane splitPane;
    private JTabbedPane tabbedPanel;
    // End of variables declaration//GEN-END:variables
}
