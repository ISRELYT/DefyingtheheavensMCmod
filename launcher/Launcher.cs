using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Text;
using System.Threading;
using System.Windows.Forms;

internal static class Program
{
    [STAThread]
    private static void Main()
    {
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        Application.Run(new LauncherForm());
    }
}

internal sealed class LauncherForm : Form
{
    private readonly string root = AppDomain.CurrentDomain.BaseDirectory;
    private readonly Label status = new Label();
    private readonly TextBox output = new TextBox();
    private readonly Button play = new Button();
    private readonly Button logs = new Button();
    private readonly ProgressBar progress = new ProgressBar();
    private Process session;
    private StreamWriter log;
    private bool active;
    private readonly object logLock = new object();

    internal LauncherForm()
    {
        Text = "Defying The Heavens";
        ClientSize = new Size(800, 520);
        MinimumSize = new Size(680, 450);
        StartPosition = FormStartPosition.CenterScreen;
        Font = new Font("Segoe UI", 10);
        BackColor = Color.FromArgb(22, 25, 32);
        ForeColor = Color.FromArgb(235, 233, 222);

        var title = new Label { Text = "DEFYING THE HEAVENS", AutoSize = true,
            Location = new Point(22, 20), Font = new Font("Segoe UI", 19, FontStyle.Bold),
            ForeColor = Color.FromArgb(225, 188, 111) };
        var subtitle = new Label { Text = "Minecraft 1.20.1  |  Cultivate. Break through. Ascend.",
            AutoSize = true, Location = new Point(24, 66) };
        status.SetBounds(24, 103, 750, 27);
        status.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
        status.Text = "Starting...";
        progress.SetBounds(24, 135, 752, 5);
        progress.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
        progress.Style = ProgressBarStyle.Marquee;

        output.SetBounds(24, 155, 752, 293);
        output.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
        output.Multiline = true;
        output.ReadOnly = true;
        output.WordWrap = false;
        output.ScrollBars = ScrollBars.Both;
        output.BackColor = Color.FromArgb(14, 17, 23);
        output.ForeColor = Color.FromArgb(195, 210, 220);
        output.Font = new Font("Consolas", 9);

        play.Text = "Play again";
        play.BackColor = Color.FromArgb(49, 43, 32);
        play.ForeColor = Color.FromArgb(245, 214, 150);
        play.FlatStyle = FlatStyle.Flat;
        play.SetBounds(24, 467, 140, 32);
        play.Anchor = AnchorStyles.Bottom | AnchorStyles.Left;
        play.Click += delegate { StartGame(); };
        logs.Text = "Open logs";
        logs.BackColor = Color.FromArgb(35, 40, 51);
        logs.ForeColor = Color.FromArgb(225, 230, 240);
        logs.FlatStyle = FlatStyle.Flat;
        logs.SetBounds(176, 467, 140, 32);
        logs.Anchor = AnchorStyles.Bottom | AnchorStyles.Left;
        logs.Click += delegate {
            string folder = Path.Combine(root, ".launcher", "logs");
            Directory.CreateDirectory(folder);
            Process.Start(new ProcessStartInfo(folder) { UseShellExecute = true });
        };
        var hint = new Label { Text = "Keep this window open while playing.", AutoSize = true,
            Location = new Point(335, 474), Anchor = AnchorStyles.Bottom | AnchorStyles.Left };
        Controls.AddRange(new Control[] { title, subtitle, status, progress, output, play, logs, hint });
        Shown += delegate { StartGame(); };
        FormClosing += OnClosing;
    }

    private void StartGame()
    {
        if (active) return;
        string script = Path.Combine(root, "launcher", "Start-Mod.ps1");
        if (!File.Exists(script) || !File.Exists(Path.Combine(root, "gradlew.bat"))) {
            status.Text = "Project files are missing.";
            MessageBox.Show(this, "Keep this EXE in the mod folder beside gradlew.bat and the launcher folder.",
                Text, MessageBoxButtons.OK, MessageBoxIcon.Error);
            progress.Style = ProgressBarStyle.Blocks;
            return;
        }
        try {
            string folder = Path.Combine(root, ".launcher", "logs");
            Directory.CreateDirectory(folder);
            string file = DateTime.Now.ToString("yyyyMMdd-HHmmss-fff") + ".log";
            log = new StreamWriter(Path.Combine(folder, file), false, new UTF8Encoding(false));
            log.AutoFlush = true;
            output.Clear();
            status.Text = "Preparing Minecraft...";
            progress.Style = ProgressBarStyle.Marquee;
            play.Enabled = false;
            active = true;
            var info = new ProcessStartInfo {
                FileName = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System),
                    "WindowsPowerShell", "v1.0", "powershell.exe"),
                Arguments = "-NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File \"" + script + "\"",
                WorkingDirectory = root,
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            };
            // A launch from PowerShell 7 can inherit its incompatible module path.
            // Let Windows PowerShell construct its own standard module search path.
            info.EnvironmentVariables.Remove("PSModulePath");
            session = new Process { StartInfo = info };
            session.OutputDataReceived += delegate(object sender, DataReceivedEventArgs e) { Receive(e.Data); };
            session.ErrorDataReceived += delegate(object sender, DataReceivedEventArgs e) { Receive(e.Data); };
            session.Start();
            session.BeginOutputReadLine();
            session.BeginErrorReadLine();
            Process running = session;
            ThreadPool.QueueUserWorkItem(delegate {
                running.WaitForExit(); // Also drains the asynchronous output streams.
                int code = running.ExitCode;
                BeginInvoke((Action)delegate { Finished(code); });
            });
        } catch (Exception ex) {
            Receive("ERROR: " + ex.Message);
            Finished(1);
        }
    }

    private void Receive(string line)
    {
        if (line == null) return;
        lock (logLock) {
            if (log != null) log.WriteLine(line);
        }
        if (IsDisposed || !IsHandleCreated) return;
        BeginInvoke((Action)delegate {
            if (output.TextLength > 180000) output.Clear();
            output.AppendText(line + Environment.NewLine);
            if (line.StartsWith("STATUS:")) status.Text = line.Substring(7);
            if (line.Contains("[Render thread/INFO]") && line.Contains("Backend library"))
                status.Text = "Minecraft is opening...";
            if (line.Contains("[Render thread/INFO]") && line.Contains("OpenAL initialized"))
                status.Text = "Minecraft is running. Enjoy your cultivation journey.";
        });
    }

    private void Finished(int code)
    {
        active = false;
        play.Enabled = true;
        progress.Style = ProgressBarStyle.Blocks;
        status.Text = code == 0 ? "Minecraft has closed. Ready to play again." : "Launch failed. Check the details below or open the logs.";
        lock (logLock) {
            if (log != null) { log.Dispose(); log = null; }
        }
        if (session != null) { session.Dispose(); session = null; }
    }

    private void OnClosing(object sender, FormClosingEventArgs e)
    {
        if (!active) return;
        // Never terminate Minecraft while it may be saving a world.
        e.Cancel = true;
        WindowState = FormWindowState.Minimized;
    }
}
