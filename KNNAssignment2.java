import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class KNNAssignment2 extends JPanel {
	private static final int WIDTH = 900;
	private static final int HEIGHT = 620;
	private static final int SAFE_MARGIN = 20;
	private static final int DEFAULT_K = 5;
	private static final int MAX_K = 15;
	private static final int CLASS_A_COUNT = 40;
	private static final int CLASS_B_COUNT = 40;

	private static final String EXIT_A = "出口A";
	private static final String EXIT_B = "出口B";
	private static final String EXIT_C = "出口C";

	private final Random random = new Random();
	private final List<Resident> residents = new ArrayList<>();
	private final Point2 investigator = new Point2(430, 310);
	private int k = DEFAULT_K;

	public KNNAssignment2() {
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setBackground(new Color(245, 248, 252));
		setOpaque(true);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				moveInvestigator(e.getX(), e.getY());
			}
		});

		regenerateData();
	}

	public void setK(int value) {
		k = Math.max(1, Math.min(value, MAX_K));
		repaint();
	}

	public void regenerateData() {
		residents.clear();

		for (int i = 0; i < CLASS_A_COUNT; i++) {
			residents.add(new Resident(
					random.nextInt(360),
					random.nextInt(260),
					EXIT_A,
					new Color(70, 115, 255)
			));
		}

		for (int i = 0; i < CLASS_B_COUNT; i++) {
			residents.add(new Resident(
					500 + random.nextInt(330),
					340 + random.nextInt(230),
					EXIT_B,
					new Color(35, 170, 85)
			));
		}

		// 調查員附近的局部偏差群聚。
		residents.add(new Resident(460, 320, EXIT_C, new Color(220, 45, 45)));
		residents.add(new Resident(400, 275, EXIT_C, new Color(220, 45, 45)));
		residents.add(new Resident(450, 260, EXIT_C, new Color(220, 45, 45)));
		residents.add(new Resident(390, 335, EXIT_C, new Color(220, 45, 45)));
		residents.add(new Resident(445, 305, EXIT_C, new Color(220, 45, 45)));

		repaint();
	}

	private void moveInvestigator(int x, int y) {
		investigator.x = clamp(x, SAFE_MARGIN, WIDTH - SAFE_MARGIN);
		investigator.y = clamp(y, SAFE_MARGIN, HEIGHT - SAFE_MARGIN);
		repaint();
	}

	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(value, max));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		residents.forEach(r -> r.dist = Math.hypot(r.x - investigator.x, r.y - investigator.y));
		List<Resident> sorted = residents.stream()
				.sorted(Comparator.comparingDouble(r -> r.dist))
				.collect(Collectors.toList());

		int usedK = Math.min(k, sorted.size());
		Map<String, Integer> votes = new HashMap<>();
		Map<String, Double> weights = new HashMap<>();

		for (Resident r : residents) {
			g2.setColor(r.color);
			g2.fillOval((int) r.x - 4, (int) r.y - 4, 8, 8);
		}

		for (int i = 0; i < usedK; i++) {
			Resident n = sorted.get(i);

			votes.put(n.choice, votes.getOrDefault(n.choice, 0) + 1);
			double w = 1.0 / (n.dist * n.dist + 1.0);
			weights.put(n.choice, weights.getOrDefault(n.choice, 0.0) + w);

			g2.setColor(new Color(120, 120, 120, 150));
			g2.drawLine(investigator.x, investigator.y, (int) n.x, (int) n.y);
		}

		String standardResult = maxKeyByValue(votes);
		String weightedResult = maxKeyByValue(weights);
		Ranking standardRanking = getRanking(votes);
		Ranking weightedRanking = getRanking(weights);

		g2.setColor(Color.YELLOW);
		g2.fillOval(investigator.x - 10, investigator.y - 10, 20, 20);
		g2.setColor(Color.BLACK);
		g2.drawOval(investigator.x - 10, investigator.y - 10, 20, 20);

		drawPanel(g2, 18, 20, "模式一：標準 KNN", standardResult, standardResult.equals(EXIT_C), standardRanking.gap, confidenceByGap(standardRanking.gap, false));
		drawPanel(g2, 18, 100, "模式二：距離加權 KNN", weightedResult, weightedResult.equals(EXIT_C), weightedRanking.gap, confidenceByGap(weightedRanking.relativeGap(), true));

		g2.setColor(new Color(45, 45, 45));
		g2.setFont(new Font("Consolas", Font.PLAIN, 13));
		g2.drawString("目前 K 值 = " + usedK + "  |  點擊畫面可移動調查員", 18, 190);
		g2.drawString("Top-K 票數：" + votes, 18, 212);
		g2.drawString("Top-K 加權分數：" + formatWeights(weights), 18, 234);
		g2.drawString("差異值(標準)：" + String.format(Locale.US, "%.0f", standardRanking.gap)
				+ "  |  差異值(加權)：" + String.format(Locale.US, "%.4f", weightedRanking.gap), 18, 256);

		g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
		g2.drawString("觀察：標準 KNN 可能被局部密集的雜訊點（出口C）誤導。", 18, HEIGHT - 46);
		g2.drawString("距離加權 KNN 因為更近的點權重較高，所以通常更穩健。", 18, HEIGHT - 24);
	}

	private void drawPanel(Graphics2D g2, int x, int y, String title, String result, boolean danger, double gap, String confidence) {
		g2.setColor(Color.BLACK);
		g2.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
		g2.drawString(title, x, y);

		g2.setColor(danger ? new Color(200, 30, 30) : new Color(35, 145, 70));
		g2.drawString("決策結果：" + result + (danger ? "（風險）" : "（安全）"), x, y + 24);
		g2.setColor(new Color(50, 50, 50));
		g2.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
		g2.drawString("差異值：" + String.format(Locale.US, "%.4f", gap) + "  |  信心等級：" + confidence, x, y + 44);
	}

	private Ranking getRanking(Map<String, ? extends Number> map) {
		List<Map.Entry<String, ? extends Number>> ordered = map.entrySet().stream()
				.sorted((a, b) -> Double.compare(b.getValue().doubleValue(), a.getValue().doubleValue()))
				.collect(Collectors.toList());

		if (ordered.isEmpty()) {
			return new Ranking("N/A", 0.0, 0.0);
		}

		double first = ordered.get(0).getValue().doubleValue();
		double second = ordered.size() > 1 ? ordered.get(1).getValue().doubleValue() : 0.0;
		return new Ranking(ordered.get(0).getKey(), first, second);
	}

	private String confidenceByGap(double gap, boolean weightedMode) {
		if (!weightedMode) {
			if (gap >= 2.0) {
				return "高";
			}
			if (gap >= 1.0) {
				return "中";
			}
			return "低";
		}

		if (gap >= 0.35) {
			return "高";
		}
		if (gap >= 0.15) {
			return "中";
		}
		return "低";
	}

	private String maxKeyByValue(Map<String, ? extends Number> map) {
		return map.entrySet().stream()
				.max((a, b) -> Double.compare(a.getValue().doubleValue(), b.getValue().doubleValue()))
				.map(Map.Entry::getKey)
				.orElse("N/A");
	}

	private String formatWeights(Map<String, Double> weights) {
		return weights.entrySet().stream()
				.sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
				.map(e -> e.getKey() + "=" + String.format(Locale.US, "%.4f", e.getValue()))
				.collect(Collectors.joining("  "));
	}

	static class Resident {
		final double x;
		final double y;
		final String choice;
		final Color color;
		double dist;

		Resident(double x, double y, String choice, Color color) {
			this.x = x;
			this.y = y;
			this.choice = choice;
			this.color = color;
		}
	}

	static class Point2 {
		int x;
		int y;

		Point2(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	static class Ranking {
		final String winner;
		final double first;
		final double second;
		final double gap;

		Ranking(String winner, double first, double second) {
			this.winner = winner;
			this.first = first;
			this.second = second;
			this.gap = first - second;
		}

		double relativeGap() {
			if (first <= 0.0) {
				return 0.0;
			}
			return gap / first;
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			KNNAssignment2 panel = new KNNAssignment2();

			JFrame frame = new JFrame("Assignment 2 - Distance Decision with KNN");
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);

			JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

			JLabel kLabel = new JLabel("K 值：" + DEFAULT_K);
			JSlider slider = new JSlider(1, MAX_K, DEFAULT_K);
			slider.setMajorTickSpacing(2);
			slider.setPaintTicks(true);
			slider.addChangeListener(e -> {
				int value = slider.getValue();
				panel.setK(value);
				kLabel.setText("K 值：" + value);
			});

			JButton regenerateButton = new JButton("重生資料");
			regenerateButton.addActionListener(e -> panel.regenerateData());

			controls.add(kLabel);
			controls.add(slider);
			controls.add(regenerateButton);

			frame.add(controls, BorderLayout.NORTH);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
		});
	}
}
