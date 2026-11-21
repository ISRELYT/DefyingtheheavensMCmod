import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The effects work's checker (the VFX plan's step A, 2026-11-21): it turns a run of /vfxcapture (the mod's VfxCapture and VfxRecorder, in a
 * game started with -Ddth.dev=true) into what the review gate reads, and checks it. One file, no libraries, like spell-art. Run from the
 * repository's root:
 * <pre>
 *   java tools/vfx-check/VfxCheck.java build &lt;run folder&gt; [--out &lt;folder&gt;] [--ref &lt;breakthrough-frames.csv&gt;] [--ffmpeg &lt;exe&gt;]
 *   java tools/vfx-check/VfxCheck.java review &lt;out.png&gt; &lt;title&gt; &lt;rows file&gt;   (rows: "label | frame.png | frame.png ...", one a line)
 *   java tools/vfx-check/VfxCheck.java board &lt;out.png&gt; &lt;title&gt; &lt;captions file&gt; [image ...]
 *   java tools/vfx-check/VfxCheck.java calibrate &lt;png ...&gt;
 * </pre>
 * {@code build} reads the run's raw frames (they stay where the game wrote them, outside the vault) and writes into {@code --out}: a sheet
 * per act at 4 frames a second, each frame labelled with its tick; every beat's frame full size; the brightness and saturation curve at
 * 20 frames a second (CSV, and a chart against the reference's); an MP4 of the first view (FFmpeg); the matrix sheets; the frame times;
 * {@code report.md} with every check; and {@code packet.md}, the list of files the reviewers need. Brightness is the frame's mean Rec. 709
 * luma (0 black, 1 white) and saturation its mean HSV saturation, as the reference CSV measured them (checked against it: calibrate).
 * <p>
 * The checks: the palette (the hottest 2% and the main colours against the briefs'); flashes (more than 3 swings over 0.20 in any one
 * second fails); the world untouched (the server's hash of the block states within 48 blocks, before and after); the frame time (the worst
 * frame and the 1% low; perf runs: what the effect adds); the live particles (2,000 at most); the curves against the reference CSV (the white-out, the flashes, the colour
 * drain); meditation: the first person's middle third clear of anything moving, and nothing showing through a wall. And the capture's own:
 * every frame written, every tick there, nothing blank.
 */
public final class VfxCheck {
	static final Color INK = new Color(0x15171C), PANEL = new Color(0x1E2128), TEXT = new Color(0xE8E6E1), DIM = new Color(0x8A8F98),
			AMBER = new Color(0xE6A23C), GOLD = new Color(0xE6A23C), VIOLET = new Color(0xA77BFF), GREY = new Color(0xB8BCC4), RED = new Color(0xE05A5A),
			GREEN = new Color(0x3E8C72);
	static final int THUMB_W = 192, THUMB_H = 108, COLS = 8, GAP = 8;
	/** Frames to the second on the sheets (the briefs': 4), from 20 ticks a second. */
	static final int SHEET_STEP = 5;
	static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	/** The briefs' colours, for the palette check (breakthrough brief §3.4, the Style Bible's soul violet and the paths'). */
	static final Map<String, Integer> PALETTE = new LinkedHashMap<>();
	static {
		String[][] p = {{"gold shadow", "491E03"}, {"gold deep", "944807"}, {"orange", "E6810E"}, {"gold", "F3A232"}, {"gold light", "FAB452"},
				{"gold pale", "FDD168"}, {"gold bright", "FDE377"}, {"hot core", "FDF38F"}, {"cream", "FDFBD4"}, {"cool dark", "201F24"},
				{"charcoal", "2B282E"}, {"cave brown", "1D1510"}, {"cave brown light", "3A2A1C"}, {"vortex crimson", "9F3C39"}, {"plum", "653B47"},
				{"vortex core", "F8AFB2"}, {"soul deep", "564B91"}, {"soul violet", "7B5EB4"}, {"soul light", "A894E9"}, {"lavender", "E8D6FC"},
				{"soul core", "FAF0FC"}, {"mod soul violet", "9457E0"}, {"ascension", "84BBCB"}, {"ascension pale", "C8F2F6"}, {"renewal", "FACB9C"},
				{"heaven core", "FFF1C9"}, {"yin cyan", "4FE3E8"}, {"yin pale", "E6FFFF"}, {"teal", "0E3A3C"}, {"crimson", "B3122E"},
				{"black-red", "3A0A10"}, {"ember", "FF6A2A"}, {"bronze", "B87333"}, {"bronze light", "D9A066"}, {"lightning", "CFE8FF"}};
		for (String[] e : p) PALETTE.put(e[0], Integer.parseInt(e[1], 16));
	}

	public static void main(String[] args) throws Exception {
		List<String> pos = new ArrayList<>();
		Map<String, String> opt = new TreeMap<>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("--") && i + 1 < args.length) opt.put(args[i].substring(2), args[++i]);
			else pos.add(args[i]);
		}
		if (pos.isEmpty()) {
			System.out.println("VfxCheck build <run> [--out dir] [--ref csv] [--ffmpeg exe] | review <out.png> <title> <rows> | board <out.png> <title> <captions> [img...] | calibrate <png...>");
			return;
		}
		switch (pos.get(0)) {
			case "build" -> build(Path.of(pos.get(1)), opt);
			case "review" -> review(Path.of(pos.get(1)), pos.get(2), Path.of(pos.get(3)));
			case "board" -> board(Path.of(pos.get(1)), pos.get(2), Path.of(pos.get(3)), pos.subList(4, pos.size()));
			case "calibrate" -> {
				for (String f : pos.subList(1, pos.size())) {
					Frame m = new Frame();
					measure(m, ImageIO.read(Path.of(f).toFile()), 1, false);
					System.out.printf(Locale.ROOT, "%s luma %.4f p98 %.4f sat %.4f hot #%06X main #%06X%n", f, m.luma, m.p98, m.sat, m.hot, m.dom);
				}
			}
			default -> System.out.println("unknown command " + pos.get(0));
		}
	}

	// --- A run ---

	static final class Frame {
		String file;
		long gameTime;
		int tick;
		/** Mean luma, its 98th percentile, its spread (standard deviation: near 0 for a blank frame), mean saturation. */
		double luma, p98, spread, sat;
		int hot, dom;
		/** The brightest luma in each cell of a 128x72 grid over the frame (a thin bolt still lights its cells), for finding strikes, and
		 *  how much bluer than red that brightest pixel is (a bolt is blue-white; grass, sky light coming back and the HUD are not). */
		byte[] grid, blue;
	}

	record Act(String name, int from, int to) {}

	record Beat(String name, int tick) {}

	static final class View {
		String name;
		Path dir;
		Map<String, Object> client, record;
		/** t0 as this camera shows it (see loadView), the server's own tick for it, and when this camera's client heard of it. */
		long tick0, serverTick0, heardAt = Long.MIN_VALUE, plateUntil;
		/** Where (ticks from t0) the reference's white-out is set on the curve: our own white-out's, 0 for a breakthrough without a cinematic. */
		double refAlign;
		/** A cinematic on this camera's own screen (its client's record): when it began (game time) and how long it ran, its letterbox with it. */
		long cinematicStart = Long.MIN_VALUE;
		int cinematicLength;
		/** The strikes this camera shows (ticks from t0), found in its frames (strikes). */
		final List<Integer> strikes = new ArrayList<>();
		/** Each strike's share of the grid lit (how much of the screen it takes). */
		final List<Double> strikeShares = new ArrayList<>();
		boolean perf, cells, hud;
		final List<Frame> frames = new ArrayList<>();
		final List<double[]> times = new ArrayList<>();
		final List<Act> acts = new ArrayList<>();
		final List<Beat> beats = new ArrayList<>();
		String camera() {
			return str(record, "camera") + (hud ? ", with the HUD" : ", no HUD");
		}
	}

	record Check(String name, String kind, String view, String result, String numbers) {}

	static final class Report {
		final String id;
		final Map<String, Object> server;
		final Path out, raw;
		final List<Check> checks = new ArrayList<>();
		final List<String> files = new ArrayList<>(), lines = new ArrayList<>();
		Report(String id, Map<String, Object> server, Path out, Path raw) {
			this.id = id;
			this.server = server;
			this.out = out;
			this.raw = raw;
		}
		void check(String name, String kind, String view, boolean pass, String numbers) {
			checks.add(new Check(name, kind, view, pass ? "pass" : "FAIL", numbers));
		}
		void info(String name, String kind, String view, String numbers) {
			checks.add(new Check(name, kind, view, "info", numbers));
		}
		void file(String what, Path p) {
			files.add(what + ": `" + p.toAbsolutePath() + "`");
		}
	}

	static void build(Path run, Map<String, String> opt) throws Exception {
		Map<String, Object> server = obj(readJson(run.resolve("server.json")));
		String id = str(server, "id");
		Path out = Path.of(opt.getOrDefault("out", run.resolve("checked").toString())).resolve(id);
		Files.createDirectories(out);
		Report rep = new Report(id, server, out, run);
		Path ref = opt.containsKey("ref") ? Path.of(opt.get("ref")) : null;
		List<double[]> reference = ref != null && Files.exists(ref) ? readReference(ref) : List.of();
		List<View> views = new ArrayList<>();
		try (Stream<Path> dirs = Files.list(run)) {
			for (Path dir : dirs.filter(d -> Files.exists(d.resolve("client.json"))).sorted().toList()) views.add(loadView(dir, server));
		}
		views.sort(Comparator.comparingDouble((View v) -> num(v.record, "startGameTime")));
		String kind = str(server, "kind");
		boolean first = true;
		for (View v : views) {
			System.out.println("view " + v.name + ": " + v.frames.size() + " frames");
			if (v.perf) {
				perf(v, rep);
				particles(v, rep);
				continue;
			}
			if (v.cells) {
				matrix(v, rep);
				continue;
			}
			measureAll(v);
			litBeats(v);
			if (kind.equals("breakthrough")) strikes(v, rep);
			timing(v, rep);
			complete(v, rep);
			curve(v, rep, reference, kind.endsWith("breakthrough"));
			sheets(v, rep);
			beatFrames(v, rep);
			flashes(v, rep);
			frameColours(v, rep);
			palette(v, rep);
			frameTime(v, rep, false);
			particles(v, rep);
			if (kind.endsWith("meditation") && v.name.equals("first")) clarity(v, rep);
			if (kind.endsWith("wall")) wall(v, rep);
			if (first && opt.containsKey("ffmpeg")) mp4(v, rep, Path.of(opt.get("ffmpeg")));
			first = false;
		}
		// The run's own end: a run stopped or failed part-way is no baseline, whatever its frames say.
		String result = str(server, "result");
		rep.check("The run went through", "capture", "-", result.equals("done"), "the server's record says " + (result.isEmpty() ? "nothing" : result)
				+ list(server.get("notes")).stream().map(String::valueOf).filter(n -> n.startsWith("FAILED")).map(n -> " (" + n + ")").collect(Collectors.joining()));
		flashCounter(rep, reference);
		world(rep);
		writeReport(rep, views, opt);
		System.out.println("wrote " + out);
	}

	static View loadView(Path dir, Map<String, Object> server) throws IOException {
		View v = new View();
		v.dir = dir;
		v.name = dir.getFileName().toString();
		v.client = obj(readJson(dir.resolve("client.json")));
		v.record = new LinkedHashMap<>();
		for (Object o : list(server.get("views"))) {
			Map<String, Object> rec = obj(o);
			if (str(rec, "name").equals(v.name)) v.record = rec;
		}
		v.perf = bool(v.client, "perf");
		v.cells = bool(v.client, "cellsOnly");
		v.hud = bool(v.client, "hud");
		v.tick0 = v.record.containsKey("tick0") ? (long) num(v.record, "tick0") : (long) num(v.client, "startGameTime");
		v.serverTick0 = v.tick0;
		// t0 as the camera shows it: the first frame its client drew after hearing of the event (its mark's seenAt + 1: the packet is read at
		// the start of a frame's loop, the tick that follows moves the clock on, then the frame is drawn). The server's own tick for it, set
		// against each client's clock, put the same moment a tick apart from one camera to the next (round 1 of Checkpoint A's review).
		for (Object o : list(v.client.get("marks"))) {
			Map<String, Object> m = obj(o);
			if ((long) num(m, "gameTime") == v.serverTick0 && m.containsKey("seenAt")) {
				v.heardAt = (long) num(m, "seenAt");
				v.tick0 = v.heardAt + 1;
			}
		}
		v.plateUntil = (long) num(v.record, "plateUntil");
		for (Object o : list(v.record.get("acts"))) {
			Map<String, Object> a = obj(o);
			v.acts.add(new Act(str(a, "name"), (int) num(a, "from"), (int) num(a, "to")));
		}
		for (Object o : list(v.record.get("beats"))) {
			Map<String, Object> b = obj(o);
			v.beats.add(new Beat(str(b, "name"), (int) num(b, "tick")));
		}
		v.refAlign = v.record.containsKey("refAlign") ? num(v.record, "refAlign") : 0;
		// A cinematic played on this camera's own screen: a beat in the middle of each of its shots, named as its client played them.
		Map<String, Object> cinematic = v.client.containsKey("cinematic") ? obj(v.client.get("cinematic")) : null;
		if (cinematic != null && bool(cinematic, "own")) {
			v.cinematicStart = (long) num(cinematic, "startGameTime");
			v.cinematicLength = (int) num(cinematic, "length");
			long off = v.cinematicStart - v.serverTick0;
			for (Object o : list(cinematic.get("shots"))) {
				Map<String, Object> s = obj(o);
				int mid = (int) (off + ((long) num(s, "from") + (long) num(s, "to")) / 2);
				if (v.beats.stream().noneMatch(b -> Math.abs(b.tick() - mid) <= 2)) v.beats.add(new Beat("Shot " + str(s, "name"), mid));
			}
			v.beats.sort(Comparator.comparingInt(Beat::tick));
		}
		Path frames = dir.resolve("frames.csv");
		if (Files.exists(frames)) {
			List<String> lines = Files.readAllLines(frames, StandardCharsets.UTF_8);
			for (String line : lines.subList(1, lines.size())) {
				String[] c = line.split(",");
				if (c.length < 2) continue;
				Frame f = new Frame();
				f.file = c[0];
				f.gameTime = Long.parseLong(c[1]);
				f.tick = (int) (f.gameTime - v.tick0);
				if (!c[0].startsWith("cells/")) v.frames.add(f);
			}
		}
		Path times = dir.resolve("frametimes.csv");
		if (Files.exists(times)) {
			List<String> lines = Files.readAllLines(times, StandardCharsets.UTF_8);
			for (String line : lines.subList(1, lines.size())) {
				String[] c = line.split(",");
				if (c.length < 6) continue;
				// tick, ms, read back (1/0), partial tick, live particles (-1 where the log has none)
				v.times.add(new double[] {Double.parseDouble(c[1]) - v.tick0, Double.parseDouble(c[3]), Double.parseDouble(c[4]), Double.parseDouble(c[2]),
						c.length > 6 ? Double.parseDouble(c[6]) : -1});
			}
		}
		if (v.acts.isEmpty() && !v.frames.isEmpty()) v.acts.add(new Act("All", v.frames.get(0).tick, v.frames.get(v.frames.size() - 1).tick));
		return v;
	}

	// --- Measuring ---

	static void measureAll(View v) {
		v.frames.parallelStream().forEach(f -> {
			try {
				BufferedImage img = ImageIO.read(v.dir.resolve(f.file).toFile());
				if (img != null) measure(f, img, 2, letterboxed(v, f));
			} catch (IOException e) {
				f.luma = -1;
			}
		});
	}

	/**
	 * Whether this frame is a cinematic's on its own player's screen with the letterbox fully in (its 10-tick slide in and out left aside,
	 * a tick of slack each side): measured inside the bars, the picture alone, as the reference's numbers are of its picture alone (a
	 * white-out under black bars could never reach the reference's 0.975 otherwise). The strike grid keeps the whole frame.
	 */
	static boolean letterboxed(View v, Frame f) {
		if (v.cinematicStart == Long.MIN_VALUE) return false;
		long t = f.gameTime - v.cinematicStart;
		return t >= 11 && t <= v.cinematicLength - 11;
	}

	/**
	 * The frame's mean luma (Rec. 709), its 98th percentile, its mean HSV saturation, its hottest 2%'s colour and its main colour; inside a
	 * 2.35:1 letterbox's bars when {@code letterboxed}.
	 */
	static void measure(Frame f, BufferedImage img, int stride, boolean letterboxed) {
		int w = img.getWidth(), h = img.getHeight();
		int[] px = img.getRGB(0, 0, w, h, null, 0, w);
		long[] count = new long[256], sr = new long[256], sg = new long[256], sb = new long[256];
		long[] bucket = new long[4096], br = new long[4096], bg = new long[4096], bb = new long[4096];
		double sumL = 0, sumS = 0, sumL2 = 0;
		long n = 0;
		int bar = letterboxed ? (int) Math.ceil(Math.max(0, (h - w / 2.35) / 2)) + 2 : 0;
		for (int y = bar; y < h - bar; y += stride) {
			for (int x = 0; x < w; x += stride) {
				int p = px[y * w + x], r = p >> 16 & 255, g = p >> 8 & 255, b = p & 255;
				double l = 0.2126 * r + 0.7152 * g + 0.0722 * b;
				int li = (int) Math.min(255, Math.round(l));
				count[li]++;
				sr[li] += r;
				sg[li] += g;
				sb[li] += b;
				int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
				sumL += l;
				sumL2 += l * l;
				sumS += mx == 0 ? 0 : (mx - mn) / (double) mx;
				int k = (r >> 4) << 8 | (g >> 4) << 4 | (b >> 4);
				bucket[k]++;
				br[k] += r;
				bg[k] += g;
				bb[k] += b;
				n++;
			}
		}
		f.luma = sumL / n / 255.0;
		f.spread = Math.sqrt(Math.max(0, sumL2 / n - (sumL / n) * (sumL / n))) / 255.0;
		f.sat = sumS / n;
		long seen = 0, hotN = 0, hr = 0, hg = 0, hb = 0;
		long top = Math.max(1, n / 50);
		f.p98 = 1;
		for (int i = 255; i >= 0 && hotN < top; i--) {
			hotN += count[i];
			hr += sr[i];
			hg += sg[i];
			hb += sb[i];
		}
		for (int i = 0; i < 256; i++) {
			seen += count[i];
			if (seen >= n * 0.98) {
				f.p98 = i / 255.0;
				break;
			}
		}
		f.hot = rgb(hr / hotN, hg / hotN, hb / hotN);
		int best = 0;
		for (int k = 1; k < 4096; k++) if (bucket[k] > bucket[best]) best = k;
		f.dom = bucket[best] == 0 ? 0 : rgb(br[best] / bucket[best], bg[best] / bucket[best], bb[best] / bucket[best]);
		byte[] grid = new byte[GRID_W * GRID_H], blue = new byte[GRID_W * GRID_H];
		for (int gy = 0; gy < GRID_H; gy++) {
			int y0 = gy * h / GRID_H, y1 = (gy + 1) * h / GRID_H;
			for (int gx = 0; gx < GRID_W; gx++) {
				int x0 = gx * w / GRID_W, x1 = (gx + 1) * w / GRID_W, max = -1, bluer = 0;
				for (int y = y0; y < y1; y += 2) {
					for (int x = x0; x < x1; x += 2) {
						int p = px[y * w + x], l = (2126 * (p >> 16 & 255) + 7152 * (p >> 8 & 255) + 722 * (p & 255)) / 10000;
						if (l > max) {
							max = l;
							bluer = Math.max(0, (p & 255) - (p >> 16 & 255));
						}
					}
				}
				grid[gy * GRID_W + gx] = (byte) max;
				blue[gy * GRID_W + gx] = (byte) bluer;
			}
		}
		f.grid = grid;
		f.blue = blue;
	}

	static final int GRID_W = 128, GRID_H = 72;
	/**
	 * A cell lit by a strike: at least this much brighter than three ticks before, and its brightest pixel this much bluer than red. A
	 * strike: at least this share of the grid so lit. None is looked for in the tribulation's first second, while its cloud gathers.
	 */
	static final int STRIKE_RISE = 48, STRIKE_BLUE = 24, STRIKE_AFTER = 20;
	static final double STRIKE_SHARE = 0.005;

	/**
	 * The strikes this camera shows: runs of frames before t0 in which a patch of the picture turns far brighter and blue-white than three
	 * ticks before (a bolt, the flash round it, or the camera inside one), each counted once at its most lit frame. They replace the
	 * server's estimate of the first and last strikes among the beats (which only knew the tribulation's schedule, not whether a bolt was on
	 * screen), and give the reviewers' sheets their strike frames. Blue, because under a shader pack a strike darkens the scene and the
	 * light coming back after it is as sudden a brightening as the bolt (round 2 of Checkpoint A's review: a "strike" frame with none).
	 */
	static void strikes(View v, Report rep) {
		List<Frame> fs = v.frames;
		double[] share = new double[fs.size()];
		for (int i = 3; i < fs.size(); i++) {
			Frame f = fs.get(i), before = fs.get(i - 3);
			if (f.grid == null || before.grid == null || f.gameTime < v.plateUntil + STRIKE_AFTER || f.tick >= 0 || f.gameTime - before.gameTime > 4) continue;
			int lit = 0;
			for (int k = 0; k < f.grid.length; k++) {
				if ((f.grid[k] & 255) - (before.grid[k] & 255) >= STRIKE_RISE && (f.blue[k] & 255) >= STRIKE_BLUE) lit++;
			}
			share[i] = lit / (double) f.grid.length;
		}
		List<String> found = new ArrayList<>();
		for (int i = 0; i < fs.size(); i++) {
			if (share[i] < STRIKE_SHARE) continue;
			int peak = i, j = i;
			while (j + 1 < fs.size() && (share[j + 1] >= STRIKE_SHARE || j + 2 < fs.size() && share[j + 2] >= STRIKE_SHARE)) {
				j++;
				if (share[j] > share[peak]) peak = j;
			}
			v.strikes.add(fs.get(peak).tick);
			v.strikeShares.add(share[peak]);
			found.add(String.format(Locale.ROOT, "t%s (%.1f%%)", sign(fs.get(peak).tick), 100 * share[peak]));
			i = j;
		}
		rep.info("Strikes on screen", "capture", v.name, v.strikes.isEmpty() ? "none found before t0" : v.strikes.size() + ": " + String.join(", ", found));
		if (v.strikes.isEmpty()) return;
		List<Beat> beats = new ArrayList<>();
		for (Beat b : v.beats) {
			if (b.name().equals("First strike")) beats.add(new Beat(b.name(), v.strikes.get(0)));
			else if (b.name().equals("Last strikes")) {
				if (v.strikes.size() > 1) beats.add(new Beat("Strike before the last", v.strikes.get(v.strikes.size() - 2)));
				beats.add(new Beat("Last strike", v.strikes.get(v.strikes.size() - 1)));
			} else beats.add(b);
		}
		beats.sort(Comparator.comparingInt(Beat::tick));
		v.beats.clear();
		v.beats.addAll(beats);
	}

	/** Where each camera's t0 and strikes fell (game time), for the reviewers' sheets (review_rows.py) to pick the same frames. */
	static void timing(View v, Report rep) throws IOException {
		StringBuilder sb = new StringBuilder("{\"view\": \"").append(v.name).append("\", \"tick0\": ").append(v.tick0).append(", \"serverTick0\": ")
				.append(v.serverTick0).append(", \"heardAt\": ").append(v.heardAt == Long.MIN_VALUE ? "null" : String.valueOf(v.heardAt)).append(", \"strikes\": [");
		for (int i = 0; i < v.strikes.size(); i++) sb.append(i == 0 ? "" : ", ").append(v.tick0 + v.strikes.get(i));
		sb.append("], \"shares\": [");
		for (int i = 0; i < v.strikeShares.size(); i++) sb.append(i == 0 ? "" : ", ").append(String.format(Locale.ROOT, "%.4f", v.strikeShares.get(i)));
		sb.append("]}\n");
		Path file = rep.out.resolve(v.name + "-timing.json");
		Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
		rep.file(v.name + ": t0 and the strikes on screen (game time)", file);
	}

	static int rgb(long r, long g, long b) {
		return (int) (Math.max(0, Math.min(255, r)) << 16 | Math.max(0, Math.min(255, g)) << 8 | Math.max(0, Math.min(255, b)));
	}

	// --- The capture's own checks ---

	static void complete(View v, Report rep) {
		Map<String, Object> c = v.client;
		long grabs = (long) num(c, "grabs"), saved = (long) num(c, "saved"), failed = (long) num(c, "failed"), dropped = (long) num(c, "dropped");
		// Cut short: the recorder's client dropped out (its own record says so), or the server ended the run part-way (the view's record).
		boolean cut = bool(c, "cutShort") || bool(v.record, "cutShort");
		boolean written = grabs == saved && failed == 0 && dropped == 0 && !cut;
		rep.check("Every frame written", "capture", v.name, written, saved + " of " + grabs + " written, " + failed + " failed, " + dropped + " dropped"
				+ (cut ? ", the view was cut short" : ""));
		int gaps = 0, missing = 0;
		for (int i = 1; i < v.frames.size(); i++) {
			long d = v.frames.get(i).gameTime - v.frames.get(i - 1).gameTime;
			if (d > 1) {
				gaps++;
				missing += (int) (d - 1);
			}
		}
		rep.check("Every tick there", "capture", v.name, missing <= Math.max(1, v.frames.size() / 100), v.frames.size() + " frames, "
				+ missing + " ticks missing in " + gaps + " gaps");
		// A blank frame is one of a single colour (no spread of brightness), black, white or anything: a dark night frame is no blank.
		long blank = v.frames.stream().filter(f -> f.luma >= 0 && f.spread < 0.01).count();
		boolean broken = v.frames.stream().anyMatch(f -> f.luma < 0);
		Frame f0 = v.frames.isEmpty() ? null : v.frames.get(0);
		boolean firstOk = f0 != null && f0.spread >= 0.01;
		// (A white-out is a run of white frames by design: up to 5% of a view may be blank.)
		rep.check("Frames are pictures", "capture", v.name, firstOk && !broken && blank <= v.frames.size() / 20, blank + " blank frames (one colour)" + (broken ? ", some unreadable" : "")
				+ (f0 == null ? ", no frames" : String.format(Locale.ROOT, "; the first: luma %.3f, spread %.3f", f0.luma, f0.spread)));
		Map<String, Object> cam = obj(v.record.get("cameraAt"));
		if (!cam.isEmpty()) {
			double d = num(cam, "distance");
			double want = v.name.equals("side") ? 5 : str(rep.server, "kind").endsWith("breakthrough") ? 24 : 16;
			rep.check("Camera where the brief puts it", "capture", v.name, Math.abs(d - want) < 2.0,
					String.format(Locale.ROOT, "%.1f blocks from the subject's chest (brief: %.0f)", d, want));
		}
	}

	// --- Curves ---

	static List<double[]> readReference(Path csv) throws IOException {
		List<double[]> rows = new ArrayList<>();
		List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
		for (String line : lines.subList(1, lines.size())) {
			String[] c = line.split(",", -1);
			if (c.length < 9) continue;
			rows.add(new double[] {Double.parseDouble(c[2]), Double.parseDouble(c[6]), Double.parseDouble(c[8]), Double.parseDouble(c[0])});
		}
		return rows; // tick, luma, sat, frame
	}

	/** The reference's white-out begins here (f2480, tick 1653.3): the breakthrough curves are lined up on it. */
	static final double REF_WHITEOUT_TICK = 2480 * 2.0 / 3.0;

	static void curve(View v, Report rep, List<double[]> reference, boolean breakthrough) throws IOException {
		StringBuilder csv = new StringBuilder("tick,gameTime,luma,luma_p98,sat,hot_hex,dom_hex\n");
		for (Frame f : v.frames) {
			csv.append(f.tick).append(',').append(f.gameTime).append(String.format(Locale.ROOT, ",%.4f,%.4f,%.4f,#%06X,#%06X%n", f.luma, f.p98, f.sat, f.hot, f.dom));
		}
		Path file = rep.out.resolve(v.name + "-curve.csv");
		Files.writeString(file, csv, StandardCharsets.UTF_8);
		rep.file(v.name + ": brightness and saturation at 20 frames a second", file);
		if (v.frames.isEmpty()) return;
		int from = v.frames.get(0).tick, to = v.frames.get(v.frames.size() - 1).tick;
		BufferedImage img = new BufferedImage(1400, 560, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = canvas(img);
		int x0 = 70, x1 = 1370, y0 = 90, y1 = 500;
		g.setColor(TEXT);
		g.setFont(font(Font.BOLD, 20));
		g.drawString(v.name + ": brightness and saturation, 20 frames a second", 20, 32);
		g.setFont(font(Font.PLAIN, 13));
		g.setColor(DIM);
		g.drawString(rep.id + (breakthrough && !reference.isEmpty() ? "  ·  reference (grey) lined up so its white-out (f2480) sits at our "
				+ (v.refAlign == 0 ? "t0" : "own, t" + sign((int) v.refAlign)) : ""), 20, 54);
		g.setColor(PANEL);
		g.fillRect(x0, y0, x1 - x0, y1 - y0);
		g.setColor(new Color(0x2A2E36));
		for (int i = 1; i < 5; i++) g.drawLine(x0, y1 - (y1 - y0) * i / 5, x1, y1 - (y1 - y0) * i / 5);
		g.setColor(DIM);
		for (int i = 0; i <= 5; i++) g.drawString(String.format(Locale.ROOT, "%.1f", i / 5.0), 30, y1 - (y1 - y0) * i / 5 + 5);
		int span = Math.max(1, to - from);
		int stepTicks = span > 600 ? 100 : span > 200 ? 40 : 20;
		for (int t = (int) Math.ceil(from / (double) stepTicks) * stepTicks; t <= to; t += stepTicks) {
			int x = x0 + (int) ((t - from) * (long) (x1 - x0) / span);
			g.setColor(new Color(0x2A2E36));
			g.drawLine(x, y0, x, y1);
			g.setColor(DIM);
			g.drawString("t" + (t >= 0 ? "+" : "") + t, x - 14, y1 + 18);
		}
		if (breakthrough && !reference.isEmpty()) {
			g.setStroke(new BasicStroke(1.5f));
			plot(g, reference, 1, from, to, x0, x1, y0, y1, new Color(0xC9CDD4), REF_WHITEOUT_TICK - v.refAlign);
			g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[] {5, 4}, 0));
			plot(g, reference, 2, from, to, x0, x1, y0, y1, new Color(0x6D727C), REF_WHITEOUT_TICK - v.refAlign);
		}
		List<double[]> ours = new ArrayList<>();
		for (Frame f : v.frames) ours.add(new double[] {f.tick, f.luma, f.sat});
		g.setStroke(new BasicStroke(2.2f));
		plot(g, ours, 1, from, to, x0, x1, y0, y1, GOLD, 0);
		plot(g, ours, 2, from, to, x0, x1, y0, y1, VIOLET, 0);
		g.setStroke(new BasicStroke(1f));
		g.setFont(font(Font.PLAIN, 12));
		for (Beat b : v.beats) {
			if (b.tick() < from || b.tick() > to) continue;
			int x = x0 + (int) ((b.tick() - from) * (long) (x1 - x0) / span);
			g.setColor(new Color(0x5A4A2A));
			g.drawLine(x, y0, x, y1);
			g.setColor(AMBER);
			g.drawString(b.name(), x + 3, y0 + 14 + (v.beats.indexOf(b) % 4) * 14);
		}
		legend(g, 900, 30, new Object[][] {{GOLD, "our brightness"}, {VIOLET, "our saturation"}, {new Color(0xC9CDD4), "reference brightness"},
				{new Color(0x6D727C), "reference saturation (dashed)"}}, breakthrough && !reference.isEmpty());
		g.dispose();
		Path png = rep.out.resolve(v.name + "-curve.png");
		ImageIO.write(img, "png", png.toFile());
		rep.file(v.name + ": the curve chart", png);
		if (breakthrough && !reference.isEmpty()) events(v, rep, reference);
	}

	static void plot(Graphics2D g, List<double[]> rows, int col, int from, int to, int x0, int x1, int y0, int y1, Color colour, double shift) {
		g.setColor(colour);
		int span = Math.max(1, to - from);
		int px = -1, py = -1;
		for (double[] r : rows) {
			double t = r[0] - shift;
			if (t < from || t > to) {
				px = -1;
				continue;
			}
			int x = x0 + (int) ((t - from) * (x1 - x0) / span), y = y1 - (int) (Math.max(0, Math.min(1, r[col])) * (y1 - y0));
			if (px >= 0) g.drawLine(px, py, x, y);
			px = x;
			py = y;
		}
	}

	static void legend(Graphics2D g, int x, int y, Object[][] items, boolean all) {
		g.setFont(font(Font.PLAIN, 13));
		int i = 0;
		for (Object[] it : items) {
			if (!all && i >= 2) break;
			g.setColor((Color) it[0]);
			g.fillRect(x + (i % 2) * 240, y + (i / 2) * 20 - 9, 18, 4);
			g.setColor(TEXT);
			g.drawString((String) it[1], x + 24 + (i % 2) * 240, y + (i / 2) * 20 - 4);
			i++;
		}
	}

	/** The white-out, the flashes and the colour drain found in both curves, ours set against the reference's. */
	static void events(View v, Report rep, List<double[]> reference) {
		double[] refT = reference.stream().mapToDouble(r -> r[0]).toArray(), refL = reference.stream().mapToDouble(r -> r[1]).toArray(),
				refS = reference.stream().mapToDouble(r -> r[2]).toArray();
		double[] ourT = v.frames.stream().mapToDouble(f -> f.tick).toArray(), ourL = v.frames.stream().mapToDouble(f -> f.luma).toArray(),
				ourS = v.frames.stream().mapToDouble(f -> f.sat).toArray();
		double[] refWhite = whiteOut(refT, refL), ourWhite = whiteOut(ourT, ourL);
		rep.info("White-out", "effect", v.name, (ourWhite == null ? "none in ours" : String.format(Locale.ROOT, "ours: up %.1f, held %.1f, clearing %.1f ticks, at t%+.0f",
				ourWhite[1], ourWhite[2], ourWhite[3], ourWhite[0])) + (refWhite == null ? "" : String.format(Locale.ROOT,
				"; reference: up %.1f, held %.1f, clearing %.1f ticks", refWhite[1], refWhite[2], refWhite[3])));
		// The drain is looked for once the white-out has cleared (the white-out's own grey is no drain), or after the breakthrough.
		double[] refDrain = drain(refT, refS, refWhite == null ? REF_WHITEOUT_TICK : refWhite[4]), ourDrain = drain(ourT, ourS, ourWhite == null ? v.refAlign : ourWhite[4]);
		rep.info("Colour drain after the blast", "effect", v.name, (ourDrain == null ? "none in ours" : String.format(Locale.ROOT,
				"ours: %.2f to %.2f over %.0f ticks from t%+.0f", ourDrain[1], ourDrain[2], ourDrain[3], ourDrain[0]))
				+ (refDrain == null ? "" : String.format(Locale.ROOT, "; reference: %.2f to %.2f over %.0f ticks, %.0f ticks after its white-out began",
				refDrain[1], refDrain[2], refDrain[3], refDrain[0] - REF_WHITEOUT_TICK)));
		List<double[]> ourSwings = swings(ourT, ourL), refSwings = swings(refT, refL);
		// Each of our flashes, and how many ticks it lies from the reference's nearest one, the reference lined up as on the chart (its
		// white-out on ours: at t0, or where a cinematic's is).
		String offsets = ourSwings.stream().limit(12).map(s -> {
			double nearest = refSwings.stream().mapToDouble(r -> r[0] - REF_WHITEOUT_TICK + v.refAlign)
					.reduce(Double.NaN, (a, b) -> Double.isNaN(a) || Math.abs(b - s[0]) < Math.abs(a - s[0]) ? b : a);
			return String.format(Locale.ROOT, "t%+.0f", s[0]) + (Double.isNaN(nearest) ? "" : String.format(Locale.ROOT, " (%+.0f from the reference's)", s[0] - nearest));
		}).collect(Collectors.joining(", "));
		rep.info("Flashes against the reference", "effect", v.name, ourSwings.size() + " swings over 0.20 in ours" + (ourSwings.isEmpty() ? "" : ": " + offsets
				+ (ourSwings.size() > 12 ? ", ..." : "")) + "; " + refSwings.size() + " in the whole reference, the most in one second "
				+ (int) worstSecond(refSwings)[0]);
	}

	/**
	 * {start, rise, hold, clear, end} in ticks of the first white-out, as the frame-by-frame breakdown measures the reference's: its plateau
	 * (brightness 0.975 or more, 3 samples at least), its rise from the last sample under 0.65 before it, its clearing to the first under 0.6
	 * after it (measured so on the reference: up 4.7, held 18.0, clearing 6.0 ticks; the brief rounds them to 5, 17 and 7). Null if there is none.
	 */
	static double[] whiteOut(double[] t, double[] l) {
		for (int i = 0; i + 2 < l.length; i++) {
			if (l[i] >= 0.975 && l[i + 1] >= 0.975 && l[i + 2] >= 0.975) {
				int a = i;
				while (a > 0 && l[a] >= 0.65) a--;
				int b = i;
				while (b + 1 < l.length && l[b + 1] >= 0.975) b++;
				int c = b;
				while (c + 1 < l.length && l[c] >= 0.6) c++;
				return new double[] {t[a], t[i] - t[a], t[b] - t[i], t[c] - t[b], t[c]};
			}
		}
		return null;
	}

	/** {start, from, to, ticks} of the steepest fall of saturation by half or more within 40 ticks, after {@code after}, or null. */
	static double[] drain(double[] t, double[] s, double after) {
		double[] best = null;
		for (int i = 0; i < s.length; i++) {
			if (t[i] < after) continue;
			for (int j = i + 1; j < s.length && t[j] - t[i] <= 40; j++) {
				if (s[i] > 0.15 && s[j] <= s[i] * 0.5 && (best == null || s[i] - s[j] > best[1] - best[2])) best = new double[] {t[i], s[i], s[j], t[j] - t[i]};
			}
			if (best != null && t[i] > after + 120) break;
		}
		return best;
	}

	/**
	 * Swings of brightness over 0.20 (from one turning point to the next), as {tick, size}. Until the first swing both the highest and the
	 * lowest point so far are kept (either may turn out to be the first turning point); after it, one per direction: the high since a swing
	 * up, the low since a swing down. (Before round 2 of Checkpoint A's review a single running extreme, moved by any rise or fall while no
	 * direction was set, never let a swing be counted at all.)
	 */
	static List<double[]> swings(double[] t, double[] l) {
		List<double[]> out = new ArrayList<>();
		if (l.length == 0) return out;
		double hi = l[0], lo = l[0], ext = l[0];
		int dir = 0;
		for (int i = 1; i < l.length; i++) {
			if (dir == 0) {
				hi = Math.max(hi, l[i]);
				lo = Math.min(lo, l[i]);
				if (hi - l[i] >= 0.20) {
					out.add(new double[] {t[i], hi - l[i]});
					dir = -1;
					ext = l[i];
				} else if (l[i] - lo >= 0.20) {
					out.add(new double[] {t[i], l[i] - lo});
					dir = 1;
					ext = l[i];
				}
			} else if (dir > 0) {
				if (l[i] > ext) ext = l[i];
				else if (ext - l[i] >= 0.20) {
					out.add(new double[] {t[i], ext - l[i]});
					dir = -1;
					ext = l[i];
				}
			} else {
				if (l[i] < ext) ext = l[i];
				else if (l[i] - ext >= 0.20) {
					out.add(new double[] {t[i], l[i] - ext});
					dir = 1;
					ext = l[i];
				}
			}
		}
		return out;
	}

	/** The most swings inside any one second (20 ticks), and the tick that second ends on. */
	static double[] worstSecond(List<double[]> s) {
		int worst = 0;
		double at = 0;
		for (double[] end : s) {
			int n = 0;
			for (double[] o : s) if (o[0] > end[0] - 20 && o[0] <= end[0]) n++;
			if (n > worst) {
				worst = n;
				at = end[0];
			}
		}
		return new double[] {worst, at};
	}

	static void flashes(View v, Report rep) {
		double[] t = v.frames.stream().mapToDouble(f -> f.tick).toArray(), l = v.frames.stream().mapToDouble(f -> f.luma).toArray();
		List<double[]> s = swings(t, l);
		// A cinematic's run: the film (from t0, the instant the breakthrough is won) is judged; the tribulation before it, its own lightning
		// (not the film's, and drawn as it was before the cinematics), is told as information, as the palette does.
		Map<String, Object> film = v.client.containsKey("cinematic") ? obj(v.client.get("cinematic")) : null;
		if (film == null && v.name.equals("observer")) film = placeholderOfRun(rep);
		if (film != null) {
			List<double[]> before = new ArrayList<>(), during = new ArrayList<>();
			for (double[] sw : s) (sw[0] < 0 ? before : during).add(sw);
			double[] w = worstSecond(during), wb = worstSecond(before);
			rep.check("At most 3 flashes a second", "effect", v.name, w[0] <= 3, during.size() + " swings over 0.20 in the film; the most in one second "
					+ (int) w[0] + (w[0] > 0 ? String.format(Locale.ROOT, " (to t%+.0f)", w[1]) : ""));
			rep.info("Flashes before the film, not judged: the tribulation's own lightning", "effect", v.name, before.size()
					+ " swings over 0.20; the most in one second " + (int) wb[0] + (wb[0] > 0 ? String.format(Locale.ROOT, " (to t%+.0f)", wb[1]) : ""));
			return;
		}
		double[] w = worstSecond(s);
		rep.check("At most 3 flashes a second", "effect", v.name, w[0] <= 3, s.size() + " swings over 0.20; the most in one second " + (int) w[0]
				+ (w[0] > 0 ? String.format(Locale.ROOT, " (to t%+.0f)", w[1]) : ""));
	}

	/**
	 * The flash counter on a curve whose flashes are known (the reference's: 42 swings over 0.20, 4 in its worst second, as counted by hand
	 * for round 2 of Checkpoint A's review). A counter that finds none there would pass every effect whatever it flashed.
	 */
	static void flashCounter(Report rep, List<double[]> reference) {
		if (reference.isEmpty()) return;
		List<double[]> s = swings(reference.stream().mapToDouble(r -> r[0]).toArray(), reference.stream().mapToDouble(r -> r[1]).toArray());
		double[] w = worstSecond(s);
		rep.check("The flash counter finds the reference's flashes", "capture", "-", s.size() > 0 && w[0] > 0,
				s.size() + " swings over 0.20 in the reference, the most in one second " + (int) w[0]);
	}

	/**
	 * The colour families the briefs give each kind of effect (names in PALETTE): what its light may be (its hottest colours), and what the
	 * frame round it may be (its main colour: the light's own, or the dark it sits in).
	 */
	static final Map<String, List<String>> FAMILIES = Map.of(
			"breakthrough", List.of("gold shadow", "gold deep", "orange", "gold", "gold light", "gold pale", "gold bright", "hot core", "cream",
					"vortex crimson", "vortex core", "soul deep", "soul violet", "soul light", "lavender", "soul core", "mod soul violet"),
			"heaven", List.of("heaven core", "wood", "water", "fire", "earth", "metal", "metal gold", "cream", "hot core"),
			"ghost", List.of("yin cyan", "yin pale", "teal", "yin dark"),
			"demon", List.of("crimson", "black-red", "ember"),
			"body", List.of("bronze", "bronze light", "lightning"));
	static final List<String> DARKS = List.of("cool dark", "charcoal", "cave brown", "cave brown light", "plum", "yin dark", "black-red", "teal");

	/**
	 * A shot's beat (the middle of a cinematic's shot) that lands on a black frame, a blink's hold, is moved past the blink: to the first
	 * frame after it once the picture has stopped brightening (the eyes fully open), at most twelve ticks on, so the shot's own colours are
	 * shown and judged (B2a: shot 3's middle, t62, fell in the blink; its first lit frame was still mostly black).
	 */
	static void litBeats(View v) {
		for (int i = 0; i < v.beats.size(); i++) {
			Beat b = v.beats.get(i);
			if (!b.name().startsWith("Shot ")) continue;
			Frame f = nearest(v, b.tick());
			if (f == null || f.luma < 0 || f.luma >= 0.03) continue;
			Frame at = null;
			for (Frame g : v.frames) {
				if (g.tick > b.tick() && g.tick <= b.tick() + 12 && g.luma >= 0.03 && (at == null || g.tick < at.tick)) at = g;
			}
			if (at == null) continue;
			// On through the blink's rise, while each tick is brighter than the last by more than 5%.
			while (true) {
				Frame next = null;
				for (Frame g : v.frames) if (g.tick == at.tick + 1 && g.luma >= 0) next = g;
				if (next == null || next.tick > b.tick() + 12 || next.luma <= at.luma * 1.05) break;
				at = next;
			}
			v.beats.set(i, new Beat(b.name() + " (after the blink)", at.tick));
		}
	}

	/**
	 * The palette as the plan words it: each beat frame's hottest 2% and its main colour, against the briefs' colours for this effect (the
	 * light's family for the hottest; the light's or the dark it sits in for the main). Judged on the whole frame: if the sky or the grass is
	 * the brightest or the main thing on screen, the effect is not, and the check says so.
	 */
	static void frameColours(View v, Report rep) {
		String kind = str(rep.server, "kind");
		String family = kind.endsWith("breakthrough") ? "breakthrough" : str(rep.server, "path");
		List<String> light = FAMILIES.getOrDefault(family, FAMILIES.get("breakthrough"));
		List<String> main = new ArrayList<>(light);
		main.addAll(DARKS);
		StringBuilder sb = new StringBuilder();
		int off = 0, seen = 0;
		// A cinematic built act by act (step B2): only its beats before the grey box's part are judged; the rest are told as information.
		Map<String, Object> film = v.client.containsKey("cinematic") ? obj(v.client.get("cinematic")) : null;
		if (film == null && v.name.equals("observer")) film = placeholderOfRun(rep);
		double greyFrom = film != null && film.get("placeholderFrom") instanceof Number n ? n.doubleValue() : Double.MAX_VALUE;
		StringBuilder grey = new StringBuilder(), outside = new StringBuilder();
		for (Beat b : v.beats) {
			Frame f = nearest(v, b.tick());
			if (f == null || f.luma < 0 || Math.abs(f.tick - b.tick()) > 3) continue;
			// A cinematic's beats up to its first tick are the tribulation and the instant it is won (the view the film takes over from,
			// none of its own light yet), and a black frame is a blink: no colours of the effect's to judge, told as information.
			if (film != null && (b.tick() <= 0 || f.luma < 0.03)) {
				Object[] hot = nearestColour(f.hot, light), dom = nearestColour(f.dom, main);
				if (outside.length() > 0) outside.append("; ");
				outside.append(String.format(Locale.ROOT, "%s%s: hottest 2%% #%06X (%.0f from %s), main #%06X (%.0f from %s)", b.name(),
						b.tick() <= 0 ? " (before the film)" : " (a black frame)", f.hot, (double) hot[1], hot[0], f.dom, (double) dom[1], dom[0]));
				continue;
			}
			if (b.tick() >= greyFrom) {
				Object[] hot = nearestColour(f.hot, light), dom = nearestColour(f.dom, main);
				if (grey.length() > 0) grey.append("; ");
				grey.append(String.format(Locale.ROOT, "%s: hottest 2%% #%06X (%.0f from %s), main #%06X (%.0f from %s)", b.name(), f.hot, (double) hot[1], hot[0],
						f.dom, (double) dom[1], dom[0]));
				continue;
			}
			Object[] hot = nearestColour(f.hot, light), dom = nearestColour(f.dom, main);
			boolean ok = (double) hot[1] <= 25 && (double) dom[1] <= 25;
			if (!ok) off++;
			seen++;
			if (sb.length() > 0) sb.append("; ");
			sb.append(String.format(Locale.ROOT, "%s: hottest 2%% #%06X (%.0f from %s), main #%06X (%.0f from %s)", b.name(), f.hot, (double) hot[1], hot[0], f.dom,
					(double) dom[1], dom[0]));
		}
		if (grey.length() > 0) {
			rep.info("Palette from tick " + (long) greyFrom + ", not judged: still the grey box's placeholder shapes", "effect", v.name, grey.toString());
		}
		if (outside.length() > 0) {
			rep.info("Palette before the film and in black frames, not judged: the tribulation's own colours, the view taken over, a blink",
					"effect", v.name, outside.toString());
		}
		if (seen == 0) return;
		String name = "Palette: the frame's hottest 2% and main colour are the briefs' (" + family + ")", numbers = off + " of " + seen + " beats more than 25 (Lab distance) off: " + sb;
		// A cinematic whose shapes are still the grey box's stand-ins (its client's record: "placeholder") has no colours of its own to judge
		// yet: the numbers are kept, as information, until the art is in (step B2).
		Map<String, Object> cinematic = v.client.containsKey("cinematic") ? obj(v.client.get("cinematic")) : null;
		if (cinematic == null && v.name.equals("observer")) cinematic = placeholderOfRun(rep);
		if (cinematic != null && bool(cinematic, "placeholder")) rep.info(name + ", not judged: placeholder shapes", "effect", v.name, numbers);
		else rep.check(name, "effect", v.name, off == 0, numbers);
	}

	/** The run's cinematic as its player view's client recorded it (an observer's client records the cinematic it watched the same way). */
	static Map<String, Object> placeholderOfRun(Report rep) {
		try (Stream<Path> dirs = Files.list(rep.raw)) {
			for (Path d : dirs.toList()) {
				Path c = d.resolve("client.json");
				if (!Files.exists(c)) continue;
				Map<String, Object> client = obj(readJson(c));
				if (client.containsKey("cinematic")) return obj(client.get("cinematic"));
			}
		} catch (IOException e) {
			// none to be had: judged as ever
		}
		return null;
	}
	static {
		PALETTE.put("wood", 0x3CE65A);
		PALETTE.put("water", 0x3C8CFF);
		PALETTE.put("fire", 0xFF4020);
		PALETTE.put("earth", 0xD99A2B);
		PALETTE.put("metal", 0xFFFFFF);
		PALETTE.put("metal gold", 0xFFC83C);
		PALETTE.put("yin dark", 0x081A20);
	}

	/** The scene without the effect: the mean of the frames before it began (plateUntil), or null. */
	static double[] plate(View v, int[] size) throws IOException {
		double[] sum = null;
		int n = 0;
		for (Frame f : v.frames) {
			if (v.plateUntil == 0 || f.gameTime >= v.plateUntil || f.gameTime < num(v.record, "startGameTime") + 2) continue;
			BufferedImage img = ImageIO.read(v.dir.resolve(f.file).toFile());
			if (img == null) continue;
			int w = img.getWidth(), h = img.getHeight();
			int[] px = img.getRGB(0, 0, w, h, null, 0, w);
			if (sum == null) {
				sum = new double[px.length * 3];
				size[0] = w;
				size[1] = h;
			}
			for (int i = 0; i < px.length; i++) {
				sum[i * 3] += px[i] >> 16 & 255;
				sum[i * 3 + 1] += px[i] >> 8 & 255;
				sum[i * 3 + 2] += px[i] & 255;
			}
			n++;
		}
		if (sum == null) return null;
		for (int i = 0; i < sum.length; i++) sum[i] /= n;
		return sum;
	}

	/**
	 * The effect's own colours in each beat frame after it began: only the pixels it changed (more than 30 levels from the clean plate), their
	 * hottest 2% and their main colour, each named by the nearest of the briefs' colours, and both held to the family the briefs give this
	 * effect (a breakthrough's gold, soul violet and their kin; a meditation's path).
	 */
	static void palette(View v, Report rep) throws IOException {
		int[] size = new int[2];
		double[] plate = plate(v, size);
		String kind = str(rep.server, "kind");
		String family = kind.endsWith("breakthrough") ? "breakthrough" : str(rep.server, "path");
		List<String> allowed = FAMILIES.getOrDefault(family, FAMILIES.get("breakthrough"));
		if (plate == null) {
			rep.info("Palette (the effect's own pixels)", "effect", v.name, "no clean plate (no frames before the effect began)");
			return;
		}
		StringBuilder sb = new StringBuilder();
		int off = 0, seen = 0, moved = 0;
		for (Beat b : v.beats) {
			Frame f = nearest(v, b.tick());
			if (f == null || f.gameTime < v.plateUntil || Math.abs(f.tick - b.tick()) > 3) continue;
			BufferedImage img = ImageIO.read(v.dir.resolve(f.file).toFile());
			if (img == null || img.getWidth() != size[0]) continue;
			int w = img.getWidth(), h = img.getHeight();
			int[] px = img.getRGB(0, 0, w, h, null, 0, w);
			List<int[]> changed = new ArrayList<>();
			for (int i = 0; i < px.length; i += 2) {
				int r = px[i] >> 16 & 255, g = px[i] >> 8 & 255, bl = px[i] & 255;
				double d = Math.max(Math.abs(r - plate[i * 3]), Math.max(Math.abs(g - plate[i * 3 + 1]), Math.abs(bl - plate[i * 3 + 2])));
				if (d > 30) changed.add(new int[] {r, g, bl});
			}
			double coverage = changed.size() / (px.length / 2.0);
			if (sb.length() > 0) sb.append("; ");
			if (changed.size() < 40) {
				sb.append(b.name()).append(String.format(Locale.ROOT, ": nothing changed (%.2f%% of the frame)", 100 * coverage));
				continue;
			}
			if (coverage > 0.4) {
				moved++;
				sb.append(b.name()).append(String.format(Locale.ROOT, ": %.0f%% of the frame changed (the camera or the light moved; not judged)", 100 * coverage));
				continue;
			}
			changed.sort(Comparator.comparingDouble(c -> -(0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2])));
			int top = Math.max(1, changed.size() / 50);
			long hr = 0, hg = 0, hb = 0;
			for (int i = 0; i < top; i++) {
				hr += changed.get(i)[0];
				hg += changed.get(i)[1];
				hb += changed.get(i)[2];
			}
			int hot = rgb(hr / top, hg / top, hb / top);
			Map<Integer, long[]> buckets = new TreeMap<>();
			for (int[] c : changed) buckets.computeIfAbsent((c[0] >> 4) << 8 | (c[1] >> 4) << 4 | (c[2] >> 4), k -> new long[4]);
			for (int[] c : changed) {
				long[] s = buckets.get((c[0] >> 4) << 8 | (c[1] >> 4) << 4 | (c[2] >> 4));
				s[0]++;
				s[1] += c[0];
				s[2] += c[1];
				s[3] += c[2];
			}
			long[] mainB = buckets.values().stream().max(Comparator.comparingLong(s -> s[0])).orElseThrow();
			int main = rgb(mainB[1] / mainB[0], mainB[2] / mainB[0], mainB[3] / mainB[0]);
			Object[] hotName = nearestColour(hot, allowed), mainName = nearestColour(main, allowed);
			boolean ok = (double) hotName[1] <= 25 && (double) mainName[1] <= 25;
			if (!ok) off++;
			seen++;
			sb.append(String.format(Locale.ROOT, "%s (%.2f%% of the frame): hot #%06X, %.0f from %s; main #%06X, %.0f from %s", b.name(), 100 * coverage, hot,
					(double) hotName[1], hotName[0], main, (double) mainName[1], mainName[0]));
		}
		rep.info("Colours of what the effect changed", "effect", v.name, (seen == 0 && moved > 0 ? "the camera moved in every beat; " : "")
				+ (seen == 0 ? "" : off + " of " + seen + " beats more than 25 off the " + family + " colours; ") + "the pixels more than 30 levels from the scene "
				+ "before the effect (the moving body, the storm and the light change count too): " + sb);
	}

	static Object[] nearestColour(int rgb) {
		return nearestColour(rgb, new ArrayList<>(PALETTE.keySet()));
	}

	static Object[] nearestColour(int rgb, List<String> names) {
		double[] lab = lab(rgb);
		String best = "";
		double d = Double.MAX_VALUE;
		for (Map.Entry<String, Integer> e : PALETTE.entrySet()) {
			if (!names.contains(e.getKey())) continue;
			double[] o = lab(e.getValue());
			double dd = Math.sqrt(Math.pow(lab[0] - o[0], 2) + Math.pow(lab[1] - o[1], 2) + Math.pow(lab[2] - o[2], 2));
			if (dd < d) {
				d = dd;
				best = e.getKey() + " #" + String.format("%06X", e.getValue());
			}
		}
		return new Object[] {best, d};
	}

	static double[] lab(int rgb) {
		double[] c = {(rgb >> 16 & 255) / 255.0, (rgb >> 8 & 255) / 255.0, (rgb & 255) / 255.0};
		for (int i = 0; i < 3; i++) c[i] = c[i] <= 0.04045 ? c[i] / 12.92 : Math.pow((c[i] + 0.055) / 1.055, 2.4);
		double x = (0.4124 * c[0] + 0.3576 * c[1] + 0.1805 * c[2]) / 0.95047, y = 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2],
				z = (0.0193 * c[0] + 0.1192 * c[1] + 0.9505 * c[2]) / 1.08883;
		x = labF(x);
		y = labF(y);
		z = labF(z);
		return new double[] {116 * y - 16, 500 * (x - y), 200 * (y - z)};
	}

	static double labF(double t) {
		return t > 216.0 / 24389 ? Math.cbrt(t) : (24389.0 / 27 * t + 16) / 116;
	}

	static Frame nearest(View v, int tick) {
		Frame best = null;
		for (Frame f : v.frames) if (best == null || Math.abs(f.tick - tick) < Math.abs(best.tick - tick)) best = f;
		return best;
	}

	// --- Frame times ---

	/** The most particles alive at once in this view (the briefs' budget: 2,000), from the frame-time log. */
	static void particles(View v, Report rep) {
		double most = -1, at = 0;
		for (double[] t : v.times) {
			if (t.length > 4 && t[4] > most) {
				most = t[4];
				at = t[0];
			}
		}
		if (most < 0) {
			rep.info("At most 2,000 live particles", "effect", v.name, "no particle count in this log");
			return;
		}
		rep.check("At most 2,000 live particles", "effect", v.name, most <= 2000, String.format(Locale.ROOT, "the most alive at once: %.0f (t%+.0f)", most, at));
	}

	static void frameTime(View v, Report rep, boolean all) {
		double[] ms = v.times.stream().skip(1).filter(t -> all || t[2] == 0).mapToDouble(t -> t[1]).sorted().toArray();
		double[] grab = v.times.stream().skip(1).filter(t -> t[2] == 1).mapToDouble(t -> t[1]).sorted().toArray();
		if (ms.length == 0) return;
		double mean = Arrays.stream(ms).average().orElse(0), p99 = ms[(int) Math.min(ms.length - 1, Math.floor(ms.length * 0.99))], worst = ms[ms.length - 1];
		rep.info("Frame time (capture run)", "effect", v.name, String.format(Locale.ROOT, "frames without a grab: mean %.1f ms, 1%% low %.1f ms (%.0f fps), worst %.1f ms; "
				+ "frames read back: mean %.1f ms, worst %.1f ms. Capture runs are capped at %s fps and pay for the read-back: the perf runs give the effect's cost",
				mean, p99, 1000 / p99, worst, grab.length == 0 ? 0 : Arrays.stream(grab).average().orElse(0), grab.length == 0 ? 0 : grab[grab.length - 1],
				str(v.client, "fpsLimit")));
	}

	/** A perf view: the frame times without the effect and with it, the heaviest second's, and the worst frame. */
	static void perf(View v, Report rep) throws IOException {
		Map<String, Long> marks = new LinkedHashMap<>();
		for (Object o : list(v.client.get("marks"))) {
			Map<String, Object> m = obj(o);
			marks.put(str(m, "name"), (long) num(m, "gameTime"));
		}
		long without = marks.getOrDefault("without", Long.MIN_VALUE), with = marks.getOrDefault("with", Long.MAX_VALUE);
		long end = (long) num(v.record, "endGameTime");
		List<double[]> a = new ArrayList<>(), b = new ArrayList<>();
		for (double[] t : v.times.subList(Math.min(1, v.times.size()), v.times.size())) {
			double gt = t[0] + v.tick0;
			if (gt >= without + 20 && gt < with) a.add(t);
			else if (gt >= with && gt <= end) b.add(t);
		}
		double[] sa = stats(a), sb = stats(b);
		double heaviest = 0, heaviestAt = 0;
		for (double s = with; s < end; s += 20) {
			final double from = s;
			double[] w = stats(b.stream().filter(t -> t[0] + v.tick0 >= from && t[0] + v.tick0 < from + 20).toList());
			if (w[0] > 0 && w[1] > heaviest) {
				heaviest = w[1];
				heaviestAt = from - v.tick0;
			}
		}
		String line = String.format(Locale.ROOT, "without: %d frames, mean %.2f ms (%.0f fps), 1%% low %.2f ms, worst %.1f ms; with: %d frames, mean %.2f ms (%.0f fps), "
				+ "1%% low %.2f ms, worst %.1f ms; it adds %.2f ms on average and %.2f ms in its heaviest second (t%+.0f)",
				(int) sa[0], sa[1], 1000 / Math.max(0.01, sa[1]), sa[2], sa[3], (int) sb[0], sb[1], 1000 / Math.max(0.01, sb[1]), sb[2], sb[3], sb[1] - sa[1], heaviest - sa[1], heaviestAt);
		// The briefs' budgets: a breakthrough's heaviest second adds at most ~4 ms a frame; one meditating cultivator stays well under 1 ms.
		boolean meditation = str(rep.server, "kind").endsWith("meditation");
		double budget = meditation ? 1 : 4;
		rep.check("Performance: the heaviest second adds at most " + (meditation ? "1 ms (one meditating cultivator)" : "4 ms"), "effect", v.name,
				heaviest - sa[1] <= budget && sb[0] > 0, line);
		rep.check("Performance: no frame over 50 ms", "effect", v.name, sb[3] <= 50 && sb[0] > 0, String.format(Locale.ROOT, "worst frame with the effect %.1f ms", sb[3]));
		// A cinematic's own stretch, from the breakthrough to the end, the tribulation before it left out: its heaviest second and its worst
		// frame on their own (its wide shots draw far more of the world than the still view without it, which these figures include).
		Long breakthrough = marks.get("breakthrough");
		if (breakthrough != null && v.client.containsKey("cinematic")) {
			List<double[]> c = b.stream().filter(t -> t[0] + v.tick0 >= breakthrough).toList();
			double[] sc = stats(c);
			double cHeaviest = 0, cAt = 0;
			for (double s = breakthrough; s < end; s += 20) {
				final double from = s;
				double[] w = stats(c.stream().filter(t -> t[0] + v.tick0 >= from && t[0] + v.tick0 < from + 20).toList());
				if (w[0] > 0 && w[1] > cHeaviest) {
					cHeaviest = w[1];
					cAt = from - v.tick0;
				}
			}
			rep.check("Performance: the cinematic's heaviest second adds at most 4 ms", "effect", v.name, cHeaviest - sa[1] <= 4 && sc[0] > 0,
					String.format(Locale.ROOT, "the cinematic: %d frames, mean %.2f ms (%.0f fps), 1%% low %.2f ms, worst %.1f ms; it adds %.2f ms on average and "
							+ "%.2f ms in its heaviest second (t%+.0f) to the view without the effect", (int) sc[0], sc[1], 1000 / Math.max(0.01, sc[1]), sc[2], sc[3],
							sc[1] - sa[1], cHeaviest - sa[1], cAt));
			rep.check("Performance: no frame of the cinematic over 50 ms", "effect", v.name, sc[3] <= 50 && sc[0] > 0,
					String.format(Locale.ROOT, "worst frame of the cinematic %.1f ms", sc[3]));
		}
		StringBuilder csv = new StringBuilder("phase,tick,ms\n");
		for (double[] t : a) csv.append("without,").append((long) t[0]).append(',').append(t[1]).append('\n');
		for (double[] t : b) csv.append("with,").append((long) t[0]).append(',').append(t[1]).append('\n');
		Path f = rep.out.resolve(v.name + "-perf.csv");
		Files.writeString(f, csv, StandardCharsets.UTF_8);
		rep.file(v.name + ": perf frame times", f);
		rep.file(v.name + ": the raw frame-time log", v.dir.resolve("frametimes.csv"));
	}

	/** {count, mean, 1% low (the 99th percentile frame time), worst}. */
	static double[] stats(List<double[]> times) {
		if (times.isEmpty()) return new double[] {0, 0, 0, 0};
		double[] ms = times.stream().mapToDouble(t -> t[1]).sorted().toArray();
		return new double[] {ms.length, Arrays.stream(ms).average().orElse(0), ms[(int) Math.min(ms.length - 1, Math.floor(ms.length * 0.99))], ms[ms.length - 1]};
	}

	// --- Meditation's checks ---

	/** The first person's middle third: the share of its pixels that change by more than 24 levels from one frame to the next, the crosshair aside. */
	static void clarity(View v, Report rep) throws IOException {
		double worst = 0, sum = 0;
		int n = 0, worstTick = 0;
		int[] prev = null;
		int w = 0, h = 0;
		// The breath only: from t0 to the release (the client's "release" mark), not the getting up after it.
		long release = Long.MAX_VALUE;
		for (Object o : list(v.client.get("marks"))) if (str(obj(o), "name").equals("release")) release = (long) num(obj(o), "gameTime");
		for (Frame f : v.frames) {
			if (f.tick < 0 || f.gameTime >= release) continue;
			BufferedImage img = ImageIO.read(v.dir.resolve(f.file).toFile());
			if (img == null) continue;
			w = img.getWidth();
			h = img.getHeight();
			int[] px = img.getRGB(w / 3, h / 3, w / 3, h / 3, null, 0, w / 3);
			if (prev != null) {
				int changed = 0, total = 0, cw = w / 3, ch = h / 3;
				for (int y = 0; y < ch; y++) {
					for (int x = 0; x < cw; x++) {
						if (Math.abs(x - cw / 2) < 14 && Math.abs(y - ch / 2) < 14) continue; // the crosshair
						int a = px[y * cw + x], b = prev[y * cw + x];
						int d = Math.max(Math.abs((a >> 16 & 255) - (b >> 16 & 255)), Math.max(Math.abs((a >> 8 & 255) - (b >> 8 & 255)), Math.abs((a & 255) - (b & 255))));
						if (d > 24) changed++;
						total++;
					}
				}
				double share = changed / (double) total;
				sum += share;
				n++;
				if (share > worst) {
					worst = share;
					worstTick = f.tick;
				}
			}
			prev = px;
		}
		rep.check("First person: the middle third stays clear", "effect", v.name, n > 0 && worst <= 0.0025, String.format(Locale.ROOT,
				"the share of the middle third that moves from frame to frame: mean %.3f%%, worst %.3f%% (t%+d); the bar is 0.25%%", 100 * sum / Math.max(1, n), 100 * worst, worstTick));
	}

	/** Behind the wall: the wall's own pixels while the subject meditates behind it, against the same while they only stood there. */
	static void wall(View v, Report rep) throws IOException {
		Map<String, Object> wall = obj(rep.server.get("wall")), cam = obj(v.record.get("cameraAt"));
		if (wall.isEmpty() || cam.isEmpty() || v.acts.size() < 2) {
			rep.check("Nothing shows through a wall", "effect", v.name, false, "no wall in the record");
			return;
		}
		int w = (int) num(v.client, "width"), h = (int) num(v.client, "height");
		double fov = num(v.client, "fov");
		List<Object> min = list(wall.get("min")), max = list(wall.get("max"));
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			double[] p = {d((i & 1) == 0 ? min.get(0) : max.get(0)), d((i & 2) == 0 ? min.get(1) : max.get(1)), d((i & 4) == 0 ? min.get(2) : max.get(2))};
			double[] s = project(p, num(cam, "x"), num(cam, "y"), num(cam, "z"), num(cam, "yaw"), num(cam, "pitch"), fov, w, h);
			minX = Math.min(minX, s[0]);
			maxX = Math.max(maxX, s[0]);
			minY = Math.min(minY, s[1]);
			maxY = Math.max(maxY, s[1]);
		}
		int bx0 = (int) Math.max(0, minX + (maxX - minX) * 0.08), bx1 = (int) Math.min(w - 1, maxX - (maxX - minX) * 0.08),
				by0 = (int) Math.max(0, minY + (maxY - minY) * 0.08), by1 = (int) Math.min(h - 1, maxY - (maxY - minY) * 0.08);
		Act standing = v.acts.get(0), sitting = v.acts.get(1);
		double[] base = null;
		int baseN = 0;
		for (Frame f : v.frames) {
			if (f.tick < standing.from() + 5 || f.tick >= standing.to()) continue;
			int[] px = box(v, f, bx0, by0, bx1, by1);
			if (px == null) continue;
			if (base == null) base = new double[px.length * 3];
			for (int i = 0; i < px.length; i++) {
				base[i * 3] += px[i] >> 16 & 255;
				base[i * 3 + 1] += px[i] >> 8 & 255;
				base[i * 3 + 2] += px[i] & 255;
			}
			baseN++;
		}
		double worst = 0;
		int worstTick = 0, frames = 0;
		if (base != null) {
			for (Frame f : v.frames) {
				if (f.tick < sitting.from() || f.tick > sitting.to()) continue;
				int[] px = box(v, f, bx0, by0, bx1, by1);
				if (px == null) continue;
				int changed = 0;
				for (int i = 0; i < px.length; i++) {
					double dr = Math.abs((px[i] >> 16 & 255) - base[i * 3] / baseN), dg = Math.abs((px[i] >> 8 & 255) - base[i * 3 + 1] / baseN),
							db = Math.abs((px[i] & 255) - base[i * 3 + 2] / baseN);
					if (Math.max(dr, Math.max(dg, db)) > 24) changed++;
				}
				frames++;
				double share = changed / (double) px.length;
				if (share > worst) {
					worst = share;
					worstTick = f.tick;
				}
			}
		}
		rep.check("Nothing shows through a wall", "effect", v.name, frames > 0 && worst <= 0.001, String.format(Locale.ROOT,
				"the wall's middle (x %d-%d, y %d-%d) against its look before the sitting: worst %.3f%% of it changed (t%+d), over %d frames; the bar is 0.1%%",
				bx0, bx1, by0, by1, 100 * worst, worstTick, frames));
	}

	static int[] box(View v, Frame f, int x0, int y0, int x1, int y1) throws IOException {
		BufferedImage img = ImageIO.read(v.dir.resolve(f.file).toFile());
		if (img == null || x1 <= x0 || y1 <= y0) return null;
		return img.getRGB(x0, y0, x1 - x0, y1 - y0, null, 0, x1 - x0);
	}

	/** A world point on the screen of a Minecraft camera at (cx, cy, cz) turned yaw and pitch (degrees), with a vertical field of view {@code fov}. */
	static double[] project(double[] p, double cx, double cy, double cz, double yaw, double pitch, double fov, int w, double h) {
		double ya = Math.toRadians(yaw), pa = Math.toRadians(pitch);
		double[] fwd = {-Math.sin(ya) * Math.cos(pa), -Math.sin(pa), Math.cos(ya) * Math.cos(pa)};
		double[] right = {-Math.cos(ya), 0, -Math.sin(ya)};
		double[] up = {right[1] * fwd[2] - right[2] * fwd[1], right[2] * fwd[0] - right[0] * fwd[2], right[0] * fwd[1] - right[1] * fwd[0]};
		double dx = p[0] - cx, dy = p[1] - cy, dz = p[2] - cz;
		double x = dx * right[0] + dy * right[1] + dz * right[2], y = dx * up[0] + dy * up[1] + dz * up[2], z = Math.max(0.01, dx * fwd[0] + dy * fwd[1] + dz * fwd[2]);
		double t = Math.tan(Math.toRadians(fov) / 2), aspect = w / h;
		return new double[] {(x / (z * t * aspect) + 1) / 2 * w, (1 - y / (z * t)) / 2 * h};
	}

	// --- The world untouched ---

	static void world(Report rep) {
		List<Object> hashes = list(rep.server.get("world"));
		if (hashes.isEmpty()) {
			rep.check("World untouched", "effect", "all", false, "no hash in the record");
			return;
		}
		int i = 0;
		for (Object o : hashes) {
			Map<String, Object> h = obj(o);
			long changed = (long) num(h, "changed");
			String first = list(h.get("firstChanges")).stream().limit(3).map(c -> {
				Map<String, Object> m = obj(c);
				return list(m.get("at")) + " " + str(m, "was") + " -> " + str(m, "now");
			}).collect(Collectors.joining("; "));
			rep.check("World untouched", "effect", "pass " + (++i), changed == 0, String.format(Locale.ROOT, "%,d block states within %d blocks hashed before and after: %s %s",
					(long) num(h, "blocks"), (long) num(h, "radius"), changed == 0 ? "the same (" + str(h, "after") + ")" : changed + " changed", first));
		}
	}

	// --- Sheets ---

	static Graphics2D canvas(BufferedImage img) {
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setColor(INK);
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		return g;
	}

	static Font font(int style, int size) {
		return new Font("Segoe UI", style, size);
	}

	/** One sheet per act: its frames at 4 a second, each labelled with its tick (beats in amber). */
	static void sheets(View v, Report rep) throws IOException {
		int n = 0;
		for (Act act : v.acts) {
			List<Frame> picks = new ArrayList<>();
			for (int t = act.from(); t <= act.to(); t += SHEET_STEP) {
				Frame f = nearest(v, t);
				if (f != null && Math.abs(f.tick - t) <= 2 && (picks.isEmpty() || picks.get(picks.size() - 1) != f)) picks.add(f);
			}
			if (picks.isEmpty()) continue;
			String title = v.name + ": " + act.name() + " (t" + sign(act.from()) + " to t" + sign(act.to()) + ", 4 frames a second)";
			String sub = rep.id + "  ·  " + v.camera() + "  ·  " + shader(v) + "  ·  ticks from " + tick0Name(v);
			Path file = rep.out.resolve(String.format(Locale.ROOT, "%s-act%d-%s.jpg", v.name, ++n, slug(act.name())));
			writeJpg(sheet(title, sub, picks, v), file);
			rep.file(v.name + ": sheet, " + act.name(), file);
		}
	}

	static String tick0Name(View v) {
		return v.beats.stream().filter(b -> b.tick() == 0).map(Beat::name).findFirst().orElse("the loop's start");
	}

	static String shader(View v) {
		Map<String, Object> mods = obj(v.client.get("mods"));
		boolean pack = bool(v.client, "shaderPackInUse");
		return pack ? "shader pack ON (Iris " + str(mods, "iris") + ")" : mods.containsKey("iris") ? "Iris " + str(mods, "iris") + ", pack off" : "no shader pack";
	}

	static BufferedImage sheet(String title, String sub, List<Frame> frames, View v) throws IOException {
		int rows = (frames.size() + COLS - 1) / COLS, cellH = THUMB_H + 34;
		int width = COLS * THUMB_W + (COLS + 1) * GAP, height = 70 + rows * (cellH + GAP) + GAP;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = canvas(img);
		g.setColor(TEXT);
		g.setFont(font(Font.BOLD, 17));
		g.drawString(title, GAP + 2, 26);
		g.setFont(font(Font.PLAIN, 12));
		g.setColor(DIM);
		g.drawString(sub, GAP + 2, 48);
		for (int i = 0; i < frames.size(); i++) {
			Frame f = frames.get(i);
			int x = GAP + (i % COLS) * (THUMB_W + GAP), y = 62 + (i / COLS) * (cellH + GAP);
			BufferedImage src = ImageIO.read(v.dir.resolve(f.file).toFile());
			if (src != null) g.drawImage(src, x, y, THUMB_W, THUMB_H, null);
			Beat beat = v.beats.stream().filter(b -> Math.abs(b.tick() - f.tick) <= 2).findFirst().orElse(null);
			g.setFont(font(Font.PLAIN, 12));
			g.setColor(beat != null ? AMBER : TEXT);
			g.drawString("t" + sign(f.tick) + (beat != null ? " · " + beat.name() : ""), x + 2, y + THUMB_H + 15);
			g.setColor(DIM);
			g.setFont(font(Font.PLAIN, 11));
			g.drawString(String.format(Locale.ROOT, "%.2f s · luma %.2f", f.tick / 20.0, f.luma), x + 2, y + THUMB_H + 29);
		}
		g.dispose();
		return img;
	}

	/** Each beat's frame, full size, with a caption strip under it. */
	static void beatFrames(View v, Report rep) throws IOException {
		for (Beat b : v.beats) {
			Frame f = nearest(v, b.tick());
			if (f == null || Math.abs(f.tick - b.tick()) > 3) continue;
			BufferedImage src = ImageIO.read(v.dir.resolve(f.file).toFile());
			if (src == null) continue;
			BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight() + 30, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = canvas(img);
			g.drawImage(src, 0, 0, null);
			g.setFont(font(Font.PLAIN, 15));
			g.setColor(AMBER);
			g.drawString(b.name(), 8, src.getHeight() + 21);
			g.setColor(TEXT);
			g.drawString("t" + sign(f.tick) + "  ·  " + v.name + "  ·  " + v.camera() + "  ·  " + shader(v), 220, src.getHeight() + 21);
			g.dispose();
			Path file = rep.out.resolve(String.format(Locale.ROOT, "%s-beat-t%s-%s.jpg", v.name, sign(f.tick).replace("+", "p").replace("-", "m"), slug(b.name())));
			writeJpg(img, file);
			rep.file(v.name + ": beat frame, " + b.name(), file);
		}
	}

	/** A matrix view: its cells in one sheet, each named. */
	static void matrix(View v, Report rep) throws IOException {
		List<Path> cells;
		try (Stream<Path> s = Files.list(v.dir.resolve("cells"))) {
			cells = s.filter(p -> p.toString().endsWith(".png")).sorted().toList();
		}
		if (cells.isEmpty()) {
			rep.check("Matrix cells written", "capture", v.name, false, "no cells");
			return;
		}
		Map<String, String> names = new LinkedHashMap<>();
		for (Object o : list(rep.server.get("cells"))) {
			Map<String, Object> c = obj(o);
			Map<String, Object> subj = obj(c.get("subject"));
			// Each cell's own time (VfxCapture asks for it 60 ticks after the subject sits), and why the ghost's alone wears a name plate.
			names.put(str(c, "name"), str(c, "realm").toUpperCase(Locale.ROOT) + " " + str(c, "stage").replace('_', ' ') + " · " + str(c, "path")
					+ (bool(c, "meditating") ? "" : " · NOT MEDITATING") + (bool(subj, "floats") ? " · floats" : "") + " · t+60 after sitting"
					+ (str(c, "path").equals("ghost") ? " · the game draws a Ghost King's title plate even with the HUD hidden" : ""));
		}
		// Each cell the frame's middle 64% (the player at its heart, larger), 4 across for the realms, 2 across for the paths.
		int cols = cells.size() > 4 ? 4 : 2, tw = cells.size() > 4 ? 394 : 792, th = tw * 9 / 16, cellH = th + 24;
		int rows = (cells.size() + cols - 1) / cols;
		int width = cols * tw + (cols + 1) * GAP, height = 70 + rows * (cellH + GAP) + GAP;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = canvas(img);
		g.setColor(TEXT);
		g.setFont(font(Font.BOLD, 17));
		boolean realms = str(rep.server, "kind").equals("matrix") && cells.size() > 4;
		g.drawString((realms ? "Meditation, one frame per realm: " + str(rep.server, "path") + " path" : "Meditation, one frame per path: " + str(rep.server, "realm").toUpperCase(Locale.ROOT)), GAP + 2, 26);
		g.setFont(font(Font.PLAIN, 12));
		g.setColor(DIM);
		g.drawString(rep.id + "  ·  seen from 5 blocks to the side" + (v.hud ? ", with the HUD" : ", no HUD") + "  ·  " + shader(v) + "  ·  3 s after sitting  ·  the middle 64% of each frame", GAP + 2, 48);
		for (int i = 0; i < cells.size(); i++) {
			int x = GAP + (i % cols) * (tw + GAP), y = 62 + (i / cols) * (cellH + GAP);
			BufferedImage src = ImageIO.read(cells.get(i).toFile());
			if (src != null) {
				int cw = (int) (src.getWidth() * 0.64), ch = (int) (src.getHeight() * 0.64), cx = (src.getWidth() - cw) / 2, cy = (src.getHeight() - ch) / 2;
				g.drawImage(src, x, y, x + tw, y + th, cx, cy, cx + cw, cy + ch, null);
			}
			String key = cells.get(i).getFileName().toString().replace(".png", "");
			g.setColor(TEXT);
			g.setFont(font(Font.PLAIN, 13));
			g.drawString(names.getOrDefault(key, key), x + 2, y + th + 16);
		}
		g.dispose();
		Path file = rep.out.resolve(realms ? "matrix-realms.jpg" : "matrix-paths.jpg");
		writeJpg(img, file);
		rep.file(realms ? "matrix: one frame per realm" : "matrix: one frame per path", file);
		rep.check("Matrix cells written", "capture", v.name, cells.size() == names.size(), cells.size() + " cells of " + names.size());
	}

	static void mp4(View v, Report rep, Path ffmpeg) throws Exception {
		if (!Files.exists(ffmpeg) || v.frames.size() < 10) return;
		Path list = rep.raw.resolve(v.name + "-frames.txt");
		StringBuilder sb = new StringBuilder();
		for (Frame f : v.frames) {
			sb.append("file '").append(v.dir.resolve(f.file).toAbsolutePath().toString().replace("\\", "/")).append("'\nduration 0.05\n");
		}
		Files.writeString(list, sb, StandardCharsets.UTF_8);
		Path mp4 = rep.out.resolve(v.name + ".mp4");
		Process p = new ProcessBuilder(ffmpeg.toString(), "-y", "-hide_banner", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", list.toString(),
				"-vf", "fps=20,scale=960:-2", "-c:v", "libx264", "-crf", "23", "-pix_fmt", "yuv420p", mp4.toString()).inheritIO().start();
		if (p.waitFor() == 0) rep.file(v.name + ": video at 20 fps", mp4);
	}

	// --- The reviewers' sheets ---

	/** One row of a review sheet: its label, its frames ("path@caption"), and how they are shown. */
	record ReviewRow(String label, List<String> frames, double crop, double cx, double cy, int size) {}

	/**
	 * The review sheet: one row a camera and state ("label | frame | frame ..."), time running left to right. A label may start with
	 * options, each ending in ';': "crop=0.3" shows the middle 30% of each frame, "crop=0.3@0.5,0.61" 30% of it round that point (where a
	 * far camera's player stands), and "size=2" shows the frames twice as large, the row wrapping onto as many lines (round 1 of
	 * Checkpoint A's review: a player 24 blocks off was 14 pixels tall on the sheet).
	 */
	static void review(Path out, String title, Path rowsFile) throws IOException {
		List<ReviewRow> rows = new ArrayList<>();
		for (String line : Files.readAllLines(rowsFile, StandardCharsets.UTF_8)) {
			if (line.isBlank() || line.startsWith("#")) continue;
			String[] r = Arrays.stream(line.split("\\|")).map(String::trim).toArray(String[]::new);
			String label = r[0];
			double crop = 1, cx = 0.5, cy = 0.5;
			int size = 1;
			while (label.matches("^(crop|size)=[^;]*;.*")) {
				String opt = label.substring(0, label.indexOf(';')).trim();
				label = label.substring(label.indexOf(';') + 1).trim();
				if (opt.startsWith("size=")) {
					size = Integer.parseInt(opt.substring(5));
					continue;
				}
				String[] at = opt.substring(5).split("@");
				crop = Double.parseDouble(at[0]);
				if (at.length > 1) {
					cx = Double.parseDouble(at[1].split(",")[0]);
					cy = Double.parseDouble(at[1].split(",")[1]);
				}
			}
			if (crop < 1) label += cx == 0.5 && cy == 0.5 ? String.format(Locale.ROOT, "  (the middle %.0f%% of each frame)", crop * 100)
					: String.format(Locale.ROOT, "  (%.0f%% of each frame, round the player)", crop * 100);
			if (size > 1) label += "  (enlarged)";
			rows.add(new ReviewRow(label, Arrays.asList(r).subList(1, r.length), crop, cx, cy, size));
		}
		int cols = rows.stream().mapToInt(r -> r.size() == 1 ? r.frames().size() : 0).max().orElse(8);
		if (cols == 0) cols = 8;
		int tw = 200, th = 113;
		int width = 2 * GAP + cols * (tw + GAP), height = 60 + GAP;
		for (ReviewRow r : rows) height += 26 + lines(r, cols) * (lineH(r, tw) + 4);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = canvas(img);
		// "Title // note": the note goes on the line under the title, before the sheet's own reading key.
		String note = title.contains(" // ") ? title.substring(title.indexOf(" // ") + 4) + ".  " : "";
		if (!note.isEmpty()) title = title.substring(0, title.indexOf(" // "));
		g.setColor(TEXT);
		g.setFont(font(Font.BOLD, 18));
		g.drawString(title, GAP + 2, 28);
		g.setFont(font(Font.PLAIN, 12));
		g.setColor(DIM);
		g.drawString(note + "Each row is one camera and state; time runs left to right" + (rows.stream().anyMatch(r -> r.size() > 1)
				? ", an enlarged row carrying on onto its next line." : "."), GAP + 2, 48);
		int y = 60;
		for (ReviewRow r : rows) {
			g.setColor(TEXT);
			g.setFont(font(Font.BOLD, 13));
			g.drawString(r.label(), GAP + 2, y + 18);
			y += 26;
			int rtw = r.size() * tw + (r.size() - 1) * GAP, rth = rtw * 9 / 16, perLine = Math.max(1, cols / r.size());
			for (int i = 0; i < r.frames().size(); i++) {
				String[] parts = r.frames().get(i).split("@", 2);
				int x = GAP + (i % perLine) * (rtw + GAP), yy = y + (i / perLine) * (lineH(r, tw) + 4);
				BufferedImage src = ImageIO.read(Path.of(parts[0]).toFile());
				if (src != null) {
					int cw = (int) (src.getWidth() * r.crop()), ch = (int) (src.getHeight() * r.crop());
					int x0 = (int) Math.max(0, Math.min(src.getWidth() - cw, r.cx() * src.getWidth() - cw / 2.0));
					int y0 = (int) Math.max(0, Math.min(src.getHeight() - ch, r.cy() * src.getHeight() - ch / 2.0));
					g.drawImage(src, x, yy, x + rtw, yy + rth, x0, y0, x0 + cw, y0 + ch, null);
				}
				g.setColor(DIM);
				g.setFont(font(Font.PLAIN, r.size() > 1 ? 13 : 11));
				g.drawString(parts.length > 1 ? parts[1] : Path.of(parts[0]).getFileName().toString(), x + 2, yy + rth + 14);
			}
			y += lines(r, cols) * (lineH(r, tw) + 4);
		}
		g.dispose();
		writeJpg(img, out);
		System.out.println("wrote " + out);
	}

	static int lines(ReviewRow r, int cols) {
		int perLine = Math.max(1, cols / r.size());
		return Math.max(1, (r.frames().size() + perLine - 1) / perLine);
	}

	static int lineH(ReviewRow r, int tw) {
		return (r.size() * tw + (r.size() - 1) * GAP) * 9 / 16 + 20;
	}

	/** The goal board: a title, captions (one a line), and the images under them side by side, each scaled to the same height. */
	static void board(Path out, String title, Path captionsFile, List<String> images) throws IOException {
		List<String> captions = Files.readAllLines(captionsFile, StandardCharsets.UTF_8);
		List<BufferedImage> imgs = new ArrayList<>();
		for (String s : images) {
			BufferedImage i = ImageIO.read(Path.of(s).toFile());
			if (i != null) imgs.add(i);
		}
		int imgH = 520, width = 1600;
		int textH = 60 + captions.size() * 22 + 20;
		BufferedImage img = new BufferedImage(width, textH + (imgs.isEmpty() ? 0 : imgH + 20), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = canvas(img);
		g.setColor(TEXT);
		g.setFont(font(Font.BOLD, 22));
		g.drawString(title, 16, 36);
		int y = 66;
		for (String c : captions) {
			boolean head = c.startsWith("# ");
			g.setFont(font(head ? Font.BOLD : Font.PLAIN, head ? 15 : 14));
			g.setColor(head ? AMBER : TEXT);
			g.drawString(head ? c.substring(2) : c, 16, y);
			y += 22;
		}
		int x = 16;
		for (BufferedImage i : imgs) {
			int w = (int) Math.round(i.getWidth() * (double) imgH / i.getHeight());
			if (x + w > width - 16) w = width - 16 - x;
			g.drawImage(i, x, textH, w, imgH, null);
			x += w + 16;
		}
		g.dispose();
		ImageIO.write(img, "png", out.toFile());
		System.out.println("wrote " + out);
	}

	// --- The report ---

	static void writeReport(Report rep, List<View> views, Map<String, String> opt) throws IOException {
		StringBuilder md = new StringBuilder();
		Map<String, Object> s = rep.server;
		md.append("---\ntype: capture\nproject: Defying the Heavens\nrun: ").append(rep.id).append("\nkind: ").append(str(s, "kind"))
				.append("\nupdated: ").append(LocalDateTime.now().toLocalDate()).append("\n---\n");
		md.append("# Capture ").append(rep.id).append("\n\n");
		md.append("- **Asked for:** `/vfxcapture ").append(str(s, "spec")).append("`, ").append(str(s, "result")).append("\n");
		Map<String, Object> scene = obj(s.get("scene"));
		md.append(String.format(Locale.ROOT, "- **Where:** %s at %.1f, %.1f, %.1f, facing %.0f; day time %s%s%n", str(scene, "dimension"), num(scene, "x"),
				num(scene, "y"), num(scene, "z"), num(scene, "facing"), str(scene, "dayTime"), bool(scene, "raining") ? ", raining" : ""));
		md.append("- **Subject:** ").append(str(s, "subject")).append("; realm ").append(str(s, "realm")).append(", stage ").append(str(s, "stage"))
				.append(", path ").append(str(s, "path")).append("; seed ").append(str(s, "seed")).append(" (").append(str(s, "length")).append(")\n");
		for (View v : views) {
			md.append("- **View `").append(v.name).append("`:** ").append(v.camera()).append("; ").append(shader(v)).append("; ").append(str(v.client, "width"))
					.append("x").append(str(v.client, "height")).append(", fov ").append(str(v.client, "fov")).append(", render distance ")
					.append(str(v.client, "renderDistance")).append(", ").append(str(v.client, "glRenderer")).append("; raw frames in `").append(v.dir.toAbsolutePath()).append("`");
			if (v.heardAt != Long.MIN_VALUE) md.append("; t0 is the first frame this camera drew after its client heard of ").append(tick0Name(v).toLowerCase(Locale.ROOT))
					.append(" (server tick ").append(v.serverTick0).append(", heard at ").append(v.heardAt).append(", t0 = ").append(v.tick0).append(")");
			md.append("\n");
		}
		for (Object n : list(s.get("notes"))) md.append("- **Note:** ").append(n).append("\n");
		md.append("\n## Checks\n\n");
		md.append("`capture` checks are the tool's own and must pass. `effect` checks measure the effect recorded: for today's effects (the baseline) they say where today stands.\n\n");
		md.append("| Check | Kind | View | Result | Numbers |\n|---|---|---|---|---|\n");
		List<Check> sorted = new ArrayList<>(rep.checks);
		sorted.sort(Comparator.comparing((Check c) -> c.kind().equals("capture") ? 0 : 1).thenComparing(c -> c.result().equals("FAIL") ? 0 : c.result().equals("pass") ? 1 : 2));
		for (Check c : sorted) {
			md.append("| ").append(c.name()).append(" | ").append(c.kind()).append(" | ").append(c.view()).append(" | ")
					.append(c.result().equals("FAIL") ? "**FAIL**" : c.result()).append(" | ").append(c.numbers().replace("|", "/")).append(" |\n");
		}
		md.append("\n## Files\n\n");
		for (String f : rep.files) md.append("- ").append(f).append("\n");
		Path report = rep.out.resolve("report.md");
		Files.writeString(report, md, StandardCharsets.UTF_8);
		StringBuilder pk = new StringBuilder();
		pk.append("---\ntype: packet\nproject: Defying the Heavens\nrun: ").append(rep.id).append("\nupdated: ").append(LocalDateTime.now().toLocalDate()).append("\n---\n");
		pk.append("# Review packet: ").append(rep.id).append("\n\nEvery file the review gate needs from this run. Paths are absolute.\n\n");
		pk.append("- The checker's report: `").append(report.toAbsolutePath()).append("`\n");
		pk.append("- The run's record (server): `").append(rep.raw.resolve("server.json").toAbsolutePath()).append("`\n");
		for (View v : views) {
			pk.append("- `").append(v.name).append("`'s client record and frame-time log: `").append(v.dir.resolve("client.json").toAbsolutePath()).append("`, `")
					.append(v.dir.resolve("frametimes.csv").toAbsolutePath()).append("`\n");
		}
		for (String f : rep.files) pk.append("- ").append(f).append("\n");
		for (String k : new String[] {"diff", "build-log", "tests"}) {
			if (opt.containsKey(k)) pk.append("- The ").append(k.replace('-', ' ')).append(": `").append(Path.of(opt.get(k)).toAbsolutePath()).append("`\n");
		}
		if (opt.containsKey("ref")) pk.append("- The reference CSV: `").append(Path.of(opt.get("ref")).toAbsolutePath()).append("`\n");
		Files.writeString(rep.out.resolve("packet.md"), pk, StandardCharsets.UTF_8);
	}

	// --- Small things ---

	static String sign(int t) {
		return (t >= 0 ? "+" : "") + t;
	}

	static String slug(String s) {
		return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
	}

	static void writeJpg(BufferedImage img, Path file) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		ImageWriteParam param = writer.getDefaultWriteParam();
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality(0.9f);
		try (ImageOutputStream os = ImageIO.createImageOutputStream(Files.newOutputStream(file))) {
			writer.setOutput(os);
			writer.write(null, new IIOImage(img, null, null), param);
		} finally {
			writer.dispose();
		}
	}

	// --- A little JSON ---

	static Object readJson(Path p) throws IOException {
		String s = Files.readString(p, StandardCharsets.UTF_8);
		int[] i = {0};
		return value(s, i);
	}

	static Object value(String s, int[] i) {
		skip(s, i);
		char c = s.charAt(i[0]);
		if (c == '{') {
			Map<String, Object> m = new LinkedHashMap<>();
			i[0]++;
			skip(s, i);
			if (s.charAt(i[0]) == '}') {
				i[0]++;
				return m;
			}
			while (true) {
				skip(s, i);
				String k = (String) value(s, i);
				skip(s, i);
				i[0]++; // :
				m.put(k, value(s, i));
				skip(s, i);
				if (s.charAt(i[0]++) == '}') return m;
			}
		}
		if (c == '[') {
			List<Object> l = new ArrayList<>();
			i[0]++;
			skip(s, i);
			if (s.charAt(i[0]) == ']') {
				i[0]++;
				return l;
			}
			while (true) {
				l.add(value(s, i));
				skip(s, i);
				if (s.charAt(i[0]++) == ']') return l;
			}
		}
		if (c == '"') {
			StringBuilder sb = new StringBuilder();
			i[0]++;
			while (true) {
				char ch = s.charAt(i[0]++);
				if (ch == '"') return sb.toString();
				if (ch == '\\') {
					char e = s.charAt(i[0]++);
					switch (e) {
						case 'n' -> sb.append('\n');
						case 't' -> sb.append('\t');
						case 'r' -> sb.append('\r');
						case 'b' -> sb.append('\b');
						case 'f' -> sb.append('\f');
						case 'u' -> {
							sb.append((char) Integer.parseInt(s.substring(i[0], i[0] + 4), 16));
							i[0] += 4;
						}
						default -> sb.append(e);
					}
				} else {
					sb.append(ch);
				}
			}
		}
		int start = i[0];
		while (i[0] < s.length() && ",}] \n\r\t".indexOf(s.charAt(i[0])) < 0) i[0]++;
		String word = s.substring(start, i[0]);
		return switch (word) {
			case "true" -> Boolean.TRUE;
			case "false" -> Boolean.FALSE;
			case "null" -> null;
			default -> Double.parseDouble(word);
		};
	}

	static void skip(String s, int[] i) {
		while (i[0] < s.length() && Character.isWhitespace(s.charAt(i[0]))) i[0]++;
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> obj(Object o) {
		return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
	}

	@SuppressWarnings("unchecked")
	static List<Object> list(Object o) {
		return o instanceof List ? (List<Object>) o : List.of();
	}

	static String str(Map<String, Object> m, String k) {
		Object o = m.get(k);
		if (o instanceof Double d && d == Math.rint(d) && Math.abs(d) < 1e15) return String.valueOf(d.longValue());
		return o == null ? "" : String.valueOf(o);
	}

	static double num(Map<String, Object> m, String k) {
		Object o = m.get(k);
		return o instanceof Double d ? d : o instanceof String s && !s.isEmpty() ? Double.parseDouble(s) : 0;
	}

	static double d(Object o) {
		return o instanceof Double v ? v : 0;
	}

	static boolean bool(Map<String, Object> m, String k) {
		return Boolean.TRUE.equals(m.get(k));
	}
}
