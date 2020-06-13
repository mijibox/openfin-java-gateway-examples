package com.mijibox.openfin.gateway.examples;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.mijibox.openfin.gateway.OpenFinGateway;
import com.mijibox.openfin.gateway.OpenFinGatewayLauncher;
import com.mijibox.openfin.gateway.ProxyObject;
import com.mijibox.openfin.gateway.gui.OpenFinCanvas;

public class EmbeddedCanvas {

	private JFrame frame;
	private OpenFinGateway gateway;
	private OpenFinCanvas canvas;
	private String url;
	private String appUuid;
	private ProxyObject proxyWindow;

	EmbeddedCanvas(OpenFinGateway gateway) {
		this.gateway = gateway;
		this.url = "https://www.google.com";
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
		JPanel p = new JPanel(new GridBagLayout());
		JButton btnBack = new JButton("<");
		btnBack.addActionListener(ae -> {
			this.proxyWindow.invoke("navigateBack");
		});
		JButton btnForward = new JButton(">");
		btnForward.addActionListener(ae -> {
			this.proxyWindow.invoke("navigateForward");
		});
		JTextField tfUrl = new JTextField(this.url);
		ActionListener goListener = ae ->{
			this.proxyWindow.invoke("navigate", Json.createValue(tfUrl.getText()))
			.exceptionally(e -> {
				e.printStackTrace();
				return null;
			});
		};
		tfUrl.addActionListener(goListener);
		JButton btnGo = new JButton("Go");
		btnGo.addActionListener(goListener);
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 3, 3, 3);
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		p.add(btnBack, c);
		c.gridx++;
		p.add(btnForward, c);
		c.gridx++;
		c.weightx = 1;
		p.add(tfUrl, c);
		c.gridx++;
		c.weightx = 0;
		p.add(btnGo, c);
		return p;
	}

	private Canvas getOpenFinCanvas() {
		this.canvas = new OpenFinCanvas(this.gateway);
		return canvas;
	}

	private void createOpenFinApp() {
		this.appUuid = UUID.randomUUID().toString();
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
		this.gateway.invoke(true, "fin.Application.start", appOpts).thenCompose(r -> {
			return r.getProxyObject().invoke(true, "getWindow");
		}).thenApply(r -> {
			this.proxyWindow = r.getProxyObject();
			return r.getResultAsJsonObject().getJsonObject("identity");
		}).thenAccept(identity -> {
			this.canvas.embed(identity);
		}).exceptionally(e -> {
			e.printStackTrace();
			return null;
		});

//		this.canvas.embedWithAppOptions(appOpts);

//		this.canvas.embedWithManifest("https://cdn.openfin.co/demos/hello/app.json");
	}

	public static void main(String[] args) {
		try {
			new EmbeddedCanvas(OpenFinGatewayLauncher
					.newOpenFinGatewayLauncher()
					.open().toCompletableFuture().get());
		}
		catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

}
