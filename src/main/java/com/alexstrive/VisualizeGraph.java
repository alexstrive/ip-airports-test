package com.alexstrive;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.swing.mxGraphComponent;
import org.jgrapht.ext.JGraphXAdapter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class VisualizeGraph {
    public static void main(String... args) throws IOException {
        var graph = IdeaProjectsAirportsRunner.initializeGraph();
        var adapter = new JGraphXAdapter<>(graph);

        var layout = new mxCircleLayout(adapter);
        layout.execute(adapter.getDefaultParent());

        var frame = new JFrame();
        mxGraphComponent component = new mxGraphComponent(adapter);
        component.zoom(2);
        component.setDragEnabled(true);
        frame.getContentPane().add(component);
        frame.setSize(new Dimension(1920, 1080));
        frame.setPreferredSize(new Dimension(1920, 1080));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
