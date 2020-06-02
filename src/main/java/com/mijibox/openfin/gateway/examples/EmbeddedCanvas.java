package com.mijibox.openfin.gateway.examples;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.mijibox.openfin.gateway.OpenFinGateway;
import com.mijibox.openfin.gateway.OpenFinLauncher;
import com.mijibox.openfin.gateway.gui.OpenFinCanvas;

public class EmbeddedCanvas {

	private JFrame frame;
	private OpenFinGateway gateway;
	private OpenFinCanvas canvas;

	EmbeddedCanvas(OpenFinGateway gateway) {
		this.gateway = gateway;
		this.frame = new JFrame("OpenFin Embedded Example");
		this.frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.frame.setContentPane(this.initContent());
		this.frame.setPreferredSize(new Dimension(800, 600));
		this.frame.pack();
		this.frame.setLocationRelativeTo(null);
		this.frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				frame.setVisible(false);
//				canvas.unembed().exceptionally(ee -> {
//					ee.printStackTrace();
//					return null;
//				}).thenCompose(v -> {
//					return gateway.close();
//				}).thenAccept(v->{
//					frame.dispose();
//				});

				canvas.getEmbeddedWindow().invoke("close", JsonValue.TRUE).thenCompose(r -> {
					return gateway.close().thenAccept(v -> {
						frame.dispose();
					});
				});
			}
		});
		this.frame.setVisible(true);
	}

	private JPanel initContent() {
		JPanel p = new JPanel(new BorderLayout());
		p.add(this.getToolbar(), BorderLayout.NORTH);
		p.add(this.getOpenFinCanvas(), BorderLayout.CENTER);
		this.createOpenFinApp();
		return p;
	}

	private JPanel getToolbar() {
		JPanel p = new JPanel();
		return p;
	}

	private Canvas getOpenFinCanvas() {
		this.canvas = new OpenFinCanvas(this.gateway);
		return canvas;
	}

	private void createOpenFinApp() {
		String appUuid = UUID.randomUUID().toString();
		JsonObject appOpts = Json.createObjectBuilder()
				.add("uuid", appUuid)
				.add("name", appUuid)
				.add("url", "https://www.google.com")
				.add("frame", true)
				.add("defaultCentered", true)
				.add("defaultWidth", 800)
				.add("defaultHeight", 600)
				.add("resizable", true)
				.add("autoShow", false)
				.build();
		
		this.canvas.embedWithAppOptions(appOpts);

//		this.canvas.embedWithManifest("https://cdn.openfin.co/demos/hello/app.json");
	}

	public static void main(String[] args) {
		try {
			new EmbeddedCanvas(OpenFinLauncher.newOpenFinLauncherBuilder().open(null).toCompletableFuture().get());
		}
		catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

}
