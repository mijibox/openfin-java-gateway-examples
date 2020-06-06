package com.mijibox.openfin.gateway.examples;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.LockSupport;

import javax.json.Json;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.mijibox.openfin.gateway.OpenFinGateway;
import com.mijibox.openfin.gateway.OpenFinGateway.OpenFinGatewayListener;
import com.mijibox.openfin.gateway.OpenFinLauncher;
import com.mijibox.openfin.gateway.ProxyObject;

public class PlatformAPI implements OpenFinGatewayListener {

	private OpenFinGateway gateway;
	private JPanel glassPane;
	private ProxyObject platformObj;

	PlatformAPI() {
		this.createGui();
		OpenFinLauncher.newOpenFinLauncherBuilder()
				.runtimeVersion("16.83.50.9")
				.addRuntimeOption("--v=1")
				.open(this);
	}

	void createGui() {
		JFrame f = new JFrame("Platform API");
		f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (gateway != null) {
					gateway.close().thenAccept(v -> {
						SwingUtilities.invokeLater(() -> {
							f.dispose();
						});
					});
				}
			}
		});

		this.glassPane = new JPanel(new BorderLayout());
		this.glassPane.setBorder(new EmptyBorder(10, 10, 10, 10));
		this.glassPane.add(new JLabel("Loading......", JLabel.CENTER), BorderLayout.CENTER);
		f.setGlassPane(this.glassPane);
		this.glassPane.setVisible(true);

		JPanel pnlContent = new JPanel(new GridBagLayout());
		GridBagConstraints gbConst = new GridBagConstraints();
		gbConst.gridx = 0;
		gbConst.gridy = 0;
		gbConst.fill = GridBagConstraints.BOTH;
		gbConst.insets = new Insets(5, 5, 5, 5);
		gbConst.weightx = 1;
		pnlContent.add(this.createStartFromManifestPanel(), gbConst);
		gbConst.gridy++;
		pnlContent.add(this.createStartAndCreateViewPanel(), gbConst);
		gbConst.gridy++;
		gbConst.weighty = 1;
		pnlContent.add(new JLabel(), gbConst);
		f.setContentPane(pnlContent);
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	JPanel createStartFromManifestPanel() {
		JPanel p = new JPanel(new BorderLayout(5, 3));
		p.setBorder(new TitledBorder("Start From Manifest"));
		JLabel lb = new JLabel("Manifest URL: ");
		JTextField tf = new JTextField("https://openfin.github.io/golden-prototype/public.json");
		JButton btn = new JButton("Start");
		btn.addActionListener(ae -> {
			this.gateway.invoke(true, "fin.Platform.startFromManifest",
					Json.createValue("https://openfin.github.io/golden-prototype/public.json")).exceptionally(e -> {
						e.printStackTrace();
						return null;
					});
		});
		p.add(lb, BorderLayout.WEST);
		p.add(tf, BorderLayout.CENTER);
		p.add(btn, BorderLayout.EAST);
		return p;
	}

	JPanel createStartAndCreateViewPanel() {
		JPanel p = new JPanel(new BorderLayout(5, 3));
		p.setBorder(new TitledBorder("Start and Create View"));
		JLabel lb = new JLabel("URL: ");
		JTextField tf = new JTextField("https://www.google.com");
		JButton btn = new JButton("Start");
		btn.addActionListener(ae -> {
			String url = tf.getText();
			CompletableFuture<ProxyObject> platformObjFuture = new CompletableFuture<ProxyObject>();
			if (this.platformObj == null) {
				platformObjFuture = this.gateway
						.invoke(true, "fin.Platform.start",
								Json.createObjectBuilder().add("uuid", UUID.randomUUID().toString()).build())
						.thenApply(r->{
							this.platformObj = r.getProxyObject();
							this.platformObj.addListener("on", "closed", e->{
								this.platformObj = null;
							});
							return this.platformObj;
						}).toCompletableFuture();
			}
			else {
				platformObjFuture.complete(this.platformObj);
			}

			platformObjFuture.thenAccept(platformObj -> {
				this.platformObj.invoke("createView", Json.createObjectBuilder().add("url", url).build())
						.exceptionally(e -> {
							e.printStackTrace();
							return null;
						});
			}).exceptionally(e -> {
				e.printStackTrace();
				return null;
			});

		});
		p.add(lb, BorderLayout.WEST);
		p.add(tf, BorderLayout.CENTER);
		p.add(btn, BorderLayout.EAST);
		return p;
	}

	@Override
	public void onOpen(OpenFinGateway gateway) {
		this.gateway = gateway;
		SwingUtilities.invokeLater(() -> {
			this.glassPane.setVisible(false);
		});
	}

	public static void main(String[] args) {
		new PlatformAPI();
	}

}
