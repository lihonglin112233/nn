package com.dorocar.car.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.dorocar.car.R;
import com.dorocar.car.ui.MainActivity;
import com.dorocar.car.vehicle.CommandExecutor;
import com.dorocar.car.vehicle.EngineGuard;
import com.dorocar.car.vehicle.GwmRepository;
import com.dorocar.car.vehicle.GwmVehicle;
import com.dorocar.car.vehicle.HvacGuard;
import com.dorocar.car.vehicle.StatusAlertCenter;
import com.dorocar.car.vehicle.TempControl;
import com.dorocar.car.vehicle.VehicleActions;
import com.dorocar.car.vehicle.VehiclePollService;
import com.dorocar.car.vehicle.VehicleStatus;
import java.util.List;
import java.util.Locale;

/* loaded from: classes.dex */
public class MainActivity extends Activity implements GwmRepository.Listener, StatusAlertCenter.UiSink {
    private static final String KEY_POLL_ON = "poll_enabled";
    private static final String PREFS = "doro_car_ui";
    private static final int REQ_NOTIFICATION = 1002;
    private static final int[] RUN_OPTIONS = {10, 15, 20, 30};
    private View alertDot;
    private TextView btnAcOff;
    private TextView btnAcOn;
    private TextView btnDefrostFront;
    private TextView btnDefrostRear;
    private TextView btnEngineStart;
    private TextView btnEngineStop;
    private TextView btnHelp;
    private TextView btnLogin;
    private TextView btnPollToggle;
    private TextView btnRunMinutes;
    private TextView btnSendCode;
    private TextView btnSteerHeat;
    private TextView btnTempApply;
    private TextView btnTempDown;
    private TextView btnTempUp;
    private CountDownTimer codeTimer;
    private LinearLayout controlBox;
    private LinearLayout dialogContainer;
    private EditText etCode;
    private EditText etPassword;
    private EditText etPhone;
    private CommandExecutor executor;
    private LinearLayout loginBox;
    private View loginDivider;
    private GwmRepository repo;
    private ScrollView scrollDialog;
    private TextView tvAcCabin;
    private TextView tvAcHint;
    private TextView tvAcState;
    private TextView tvAlertHint;
    private TextView tvAlertTitle;
    private TextView tvBattery;
    private TextView tvEngineHint;
    private TextView tvEngineState;
    private TextView tvLock;
    private TextView tvLoginStatus;
    private TextView tvRange;
    private TextView tvTemp;
    private TextView tvTempHint;
    private TextView tvTempValue;
    private TextView tvTempZone;
    private TextView tvVehicleName;
    private final Handler main = new Handler(Looper.getMainLooper());
    private float pendingTemp = 24.0f;
    private String tempZone = TempControl.ZONE_BOTH;
    private boolean fahrenheit = false;
    private int runMinutes = 15;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        this.repo = GwmRepository.get(this);
        this.repo.addListener(this);
        this.executor = new CommandExecutor(this, this.repo);
        StatusAlertCenter.get().setUiSink(this);
        bindViews();
        bindActions();
        m27lambda$onLoginStateChanged$28$comdorocarcaruiMainActivity(this.repo.isLoggedIn());
        // 本地有凭据但尚未完成通道恢复时，先把界面切到"正在恢复登录"，避免闪一下登录框
        if (!this.repo.isLoggedIn() && this.repo.sessionState() == GwmRepository.SessionState.RESTORING) {
            this.loginBox.setVisibility(8);
            this.loginDivider.setVisibility(8);
            this.controlBox.setVisibility(8);
            this.tvVehicleName.setText("正在恢复登录…");
        }
        boolean z = getSharedPreferences(PREFS, 0).getBoolean(KEY_POLL_ON, false);
        if (z && this.repo.isLoggedIn()) {
            startMonitoring();
        }
        applyPollState(z);
    }

    private void bindViews() {
        this.tvVehicleName = (TextView) findViewById(R.id.tv_vehicle_name);
        this.btnHelp = (TextView) findViewById(R.id.btn_help);
        this.alertDot = findViewById(R.id.alert_dot);
        this.tvAlertTitle = (TextView) findViewById(R.id.tv_alert_title);
        this.tvAlertHint = (TextView) findViewById(R.id.tv_alert_hint);
        this.btnPollToggle = (TextView) findViewById(R.id.btn_poll_toggle);
        this.scrollDialog = (ScrollView) findViewById(R.id.scroll_dialog);
        this.dialogContainer = (LinearLayout) findViewById(R.id.dialog_container);
        this.tvLock = (TextView) findViewById(R.id.tv_lock);
        this.tvBattery = (TextView) findViewById(R.id.tv_battery);
        this.tvRange = (TextView) findViewById(R.id.tv_range);
        this.tvTemp = (TextView) findViewById(R.id.tv_temp);
        this.tvAcState = (TextView) findViewById(R.id.tv_ac_state);
        this.tvAcHint = (TextView) findViewById(R.id.tv_ac_hint);
        this.tvAcCabin = (TextView) findViewById(R.id.tv_ac_cabin);
        this.btnAcOn = (TextView) findViewById(R.id.btn_ac_on);
        this.btnAcOff = (TextView) findViewById(R.id.btn_ac_off);
        this.btnDefrostFront = (TextView) findViewById(R.id.btn_defrost_front);
        this.btnDefrostRear = (TextView) findViewById(R.id.btn_defrost_rear);
        this.btnSteerHeat = (TextView) findViewById(R.id.btn_steer_heat);
        this.btnTempUp = (TextView) findViewById(R.id.btn_temp_up);
        this.btnTempDown = (TextView) findViewById(R.id.btn_temp_down);
        this.btnTempApply = (TextView) findViewById(R.id.btn_temp_apply);
        this.tvTempValue = (TextView) findViewById(R.id.tv_temp_value);
        this.tvTempZone = (TextView) findViewById(R.id.tv_temp_zone);
        this.tvTempHint = (TextView) findViewById(R.id.tv_temp_hint);
        this.tvEngineState = (TextView) findViewById(R.id.tv_engine_state);
        this.tvEngineHint = (TextView) findViewById(R.id.tv_engine_hint);
        this.btnEngineStart = (TextView) findViewById(R.id.btn_engine_start);
        this.btnEngineStop = (TextView) findViewById(R.id.btn_engine_stop);
        this.btnRunMinutes = (TextView) findViewById(R.id.btn_run_minutes);
        this.loginBox = (LinearLayout) findViewById(R.id.login_box);
        this.controlBox = (LinearLayout) findViewById(R.id.control_box);
        this.loginDivider = findViewById(R.id.login_divider);
        this.etPhone = (EditText) findViewById(R.id.et_phone);
        this.etPassword = (EditText) findViewById(R.id.et_password);
        this.etCode = (EditText) findViewById(R.id.et_code);
        this.btnSendCode = (TextView) findViewById(R.id.btn_send_code);
        this.btnLogin = (TextView) findViewById(R.id.btn_login);
        this.tvLoginStatus = (TextView) findViewById(R.id.tv_login_status);
    }

    private void bindActions() {
        this.btnPollToggle.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m1lambda$bindActions$0$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnHelp.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda14
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m2lambda$bindActions$1$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnSendCode.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda18
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m13lambda$bindActions$2$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnLogin.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda19
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m17lambda$bindActions$3$comdorocarcaruiMainActivity(view);
            }
        });
        findViewById(R.id.btn_lock).setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m18lambda$bindActions$4$comdorocarcaruiMainActivity(view);
            }
        });
        findViewById(R.id.btn_unlock).setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda21
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m19lambda$bindActions$5$comdorocarcaruiMainActivity(view);
            }
        });
        findViewById(R.id.btn_whistle).setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda22
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m20lambda$bindActions$6$comdorocarcaruiMainActivity(view);
            }
        });
        findViewById(R.id.btn_flash).setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda23
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m21lambda$bindActions$7$comdorocarcaruiMainActivity(view);
            }
        });
        findViewById(R.id.btn_window).setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda24
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m22lambda$bindActions$8$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnAcOn.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda25
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m23lambda$bindActions$9$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnAcOff.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m3lambda$bindActions$10$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnDefrostFront.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m4lambda$bindActions$11$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnDefrostRear.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m5lambda$bindActions$12$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnSteerHeat.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m6lambda$bindActions$13$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnTempUp.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7lambda$bindActions$14$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnTempDown.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m8lambda$bindActions$15$comdorocarcaruiMainActivity(view);
            }
        });
        this.tvTempZone.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda10
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m9lambda$bindActions$16$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnTempApply.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m10lambda$bindActions$17$comdorocarcaruiMainActivity(view);
            }
        });
        findViewById(R.id.btn_temp_preset_24).setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m11lambda$bindActions$18$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnEngineStart.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda13
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m12lambda$bindActions$19$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnEngineStop.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda15
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m14lambda$bindActions$20$comdorocarcaruiMainActivity(view);
            }
        });
        this.btnRunMinutes.setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda16
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m15lambda$bindActions$21$comdorocarcaruiMainActivity(view);
            }
        });
        findViewById(R.id.btn_logout).setOnClickListener(new View.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m16lambda$bindActions$22$comdorocarcaruiMainActivity(view);
            }
        });
    }

    /* renamed from: lambda$bindActions$0$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m1lambda$bindActions$0$comdorocarcaruiMainActivity(View view) {
        setPollEnabled(!getSharedPreferences(PREFS, 0).getBoolean(KEY_POLL_ON, false));
    }

    /* renamed from: lambda$bindActions$1$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m2lambda$bindActions$1$comdorocarcaruiMainActivity(View view) {
        showHelp();
    }

    /* renamed from: lambda$bindActions$2$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m13lambda$bindActions$2$comdorocarcaruiMainActivity(View view) {
        sendCode();
    }

    /* renamed from: lambda$bindActions$3$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m17lambda$bindActions$3$comdorocarcaruiMainActivity(View view) {
        doLogin();
    }

    /* renamed from: lambda$bindActions$4$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m18lambda$bindActions$4$comdorocarcaruiMainActivity(View view) {
        run(VehicleActions.LOCK);
    }

    /* renamed from: lambda$bindActions$5$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m19lambda$bindActions$5$comdorocarcaruiMainActivity(View view) {
        run(VehicleActions.UNLOCK);
    }

    /* renamed from: lambda$bindActions$6$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m20lambda$bindActions$6$comdorocarcaruiMainActivity(View view) {
        run("WHISTLE");
    }

    /* renamed from: lambda$bindActions$7$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m21lambda$bindActions$7$comdorocarcaruiMainActivity(View view) {
        run("FLASH");
    }

    /* renamed from: lambda$bindActions$8$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m22lambda$bindActions$8$comdorocarcaruiMainActivity(View view) {
        run("WINDOW_CLOSE");
    }

    /* renamed from: lambda$bindActions$9$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m23lambda$bindActions$9$comdorocarcaruiMainActivity(View view) {
        runIfEngine(VehicleActions.AC_ON);
    }

    /* renamed from: lambda$bindActions$10$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m3lambda$bindActions$10$comdorocarcaruiMainActivity(View view) {
        run(VehicleActions.AC_OFF);
    }

    /* renamed from: lambda$bindActions$11$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m4lambda$bindActions$11$comdorocarcaruiMainActivity(View view) {
        runIfEngine(VehicleActions.DEFROST_FRONT);
    }

    /* renamed from: lambda$bindActions$12$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m5lambda$bindActions$12$comdorocarcaruiMainActivity(View view) {
        runIfEngine(VehicleActions.DEFROST_REAR);
    }

    /* renamed from: lambda$bindActions$13$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m6lambda$bindActions$13$comdorocarcaruiMainActivity(View view) {
        runIfEngine(VehicleActions.STEER_HEAT);
    }

    /* renamed from: lambda$bindActions$14$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m7lambda$bindActions$14$comdorocarcaruiMainActivity(View view) {
        stepTemp(1);
    }

    /* renamed from: lambda$bindActions$15$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m8lambda$bindActions$15$comdorocarcaruiMainActivity(View view) {
        stepTemp(-1);
    }

    /* renamed from: lambda$bindActions$16$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m9lambda$bindActions$16$comdorocarcaruiMainActivity(View view) {
        cycleTempZone();
    }

    /* renamed from: lambda$bindActions$17$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m10lambda$bindActions$17$comdorocarcaruiMainActivity(View view) {
        applyTemp();
    }

    /* renamed from: lambda$bindActions$18$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m11lambda$bindActions$18$comdorocarcaruiMainActivity(View view) {
        this.pendingTemp = 24.0f;
        renderTempValue();
        applyTemp();
    }

    /* renamed from: lambda$bindActions$19$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m12lambda$bindActions$19$comdorocarcaruiMainActivity(View view) {
        confirmEngineStart();
    }

    /* renamed from: lambda$bindActions$20$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m14lambda$bindActions$20$comdorocarcaruiMainActivity(View view) {
        confirmEngineStop();
    }

    /* renamed from: lambda$bindActions$21$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m15lambda$bindActions$21$comdorocarcaruiMainActivity(View view) {
        cycleRunMinutes();
    }

    /* renamed from: lambda$bindActions$22$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m16lambda$bindActions$22$comdorocarcaruiMainActivity(View view) {
        confirmLogout();
    }

    private void cycleRunMinutes() {
        int i = 0;
        int i2 = 0;
        while (true) {
            if (i2 >= RUN_OPTIONS.length) {
                break;
            }
            if (RUN_OPTIONS[i2] != this.runMinutes) {
                i2++;
            } else {
                i = i2;
                break;
            }
        }
        this.runMinutes = RUN_OPTIONS[(i + 1) % RUN_OPTIONS.length];
        this.btnRunMinutes.setText(this.runMinutes + " 分钟");
    }

    private void confirmEngineStart() {
        EngineGuard.Verdict canStart = EngineGuard.canStart(this.repo.currentStatus());
        if (!canStart.allowed) {
            addCard(canStart.message, false);
            toast(canStart.message);
        } else if (canStart.needsConfirm) {
            new AlertDialog.Builder(this).setTitle("确认远程启动？").setMessage(canStart.message + "\n\n发动机将运行 " + this.runMinutes + " 分钟。").setNegativeButton("取消", (DialogInterface.OnClickListener) null).setPositiveButton("启动", new DialogInterface.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda26
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.m24lambda$confirmEngineStart$23$comdorocarcaruiMainActivity(dialogInterface, i);
                }
            }).show();
        } else {
            doEngineStart();
        }
    }

    /* renamed from: lambda$confirmEngineStart$23$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m24lambda$confirmEngineStart$23$comdorocarcaruiMainActivity(DialogInterface dialogInterface, int i) {
        doEngineStart();
    }

    private void doEngineStart() {
        addCard("（手动）远程启动 · " + this.runMinutes + " 分钟", false);
        this.executor.run("ENGINE_START", null, this.runMinutes, resultCallback());
    }

    private void confirmEngineStop() {
        EngineGuard.Verdict canStop = EngineGuard.canStop(this.repo.currentStatus());
        if (!canStop.allowed) {
            addCard(canStop.message, false);
            toast(canStop.message);
        } else {
            new AlertDialog.Builder(this).setTitle("确认远程熄火？").setMessage("车辆将立即熄火，空调等附件也会停止工作。").setNegativeButton("取消", (DialogInterface.OnClickListener) null).setPositiveButton("熄火", new DialogInterface.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda27
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.m25lambda$confirmEngineStop$24$comdorocarcaruiMainActivity(dialogInterface, i);
                }
            }).show();
        }
    }

    /* renamed from: lambda$confirmEngineStop$24$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m25lambda$confirmEngineStop$24$comdorocarcaruiMainActivity(DialogInterface dialogInterface, int i) {
        run("ENGINE_STOP");
    }

    private void runIfEngine(String str) {
        boolean platformSupportsAux;
        String platformOf = this.repo.platformOf(this.repo.selectedVin());
        if (VehicleActions.AC_ON.equals(str)) {
            platformSupportsAux = HvacGuard.platformSupportsHvac(platformOf);
        } else {
            platformSupportsAux = HvacGuard.platformSupportsAux(platformOf);
        }
        if (!platformSupportsAux) {
            String unsupportedPlatformText = HvacGuard.unsupportedPlatformText(platformOf);
            addCard(unsupportedPlatformText, false);
            toast(unsupportedPlatformText);
            return;
        }
        HvacGuard.Verdict canTurnOn = HvacGuard.canTurnOn(this.repo.currentStatus());
        if (!canTurnOn.allowed) {
            addCard(canTurnOn.message, false);
            toast(canTurnOn.message);
        } else {
            if (VehicleActions.AC_ON.equals(str)) {
                float normalize = TempControl.normalize(this.pendingTemp);
                addCard("（手动）开空调 · 设定 " + TempControl.format(normalize), false);
                this.executor.run(str, TempControl.bodiesOf(normalize, this.tempZone), resultCallback());
                return;
            }
            run(str);
        }
    }

    private void stepTemp(int i) {
        float f = this.pendingTemp;
        float up = i > 0 ? TempControl.up(f) : TempControl.down(f);
        if (Math.abs(up - this.pendingTemp) < 0.001f) {
            toast(i > 0 ? "已到最高温" : "已到最低温");
        } else {
            this.pendingTemp = up;
            renderTempValue();
        }
    }

    private void cycleTempZone() {
        if (TempControl.ZONE_BOTH.equals(this.tempZone)) {
            this.tempZone = TempControl.ZONE_DRIVER;
        } else if (TempControl.ZONE_DRIVER.equals(this.tempZone)) {
            this.tempZone = TempControl.ZONE_PASSENGER;
        } else {
            this.tempZone = TempControl.ZONE_BOTH;
        }
        this.tvTempZone.setText(TempControl.zoneLabel(this.tempZone));
    }

    private void applyTemp() {
        String platformOf = this.repo.platformOf(this.repo.selectedVin());
        if (!HvacGuard.platformSupportsHvac(platformOf)) {
            String unsupportedPlatformText = HvacGuard.unsupportedPlatformText(platformOf);
            addCard(unsupportedPlatformText, false);
            toast(unsupportedPlatformText);
            return;
        }
        VehicleStatus currentStatus = this.repo.currentStatus();
        TempControl.Verdict canSet = TempControl.canSet(currentStatus, this.pendingTemp);
        if (!canSet.allowed) {
            addCard(canSet.message, false);
            toast(canSet.message);
            return;
        }
        float normalize = TempControl.normalize(this.pendingTemp);
        addCard("（手动）调温至 " + TempControl.format(normalize) + " · " + TempControl.zoneLabel(this.tempZone), false);
        TempControl.Response responseOf = TempControl.responseOf(currentStatus);
        if (!responseOf.verifiable) {
            addCard("· " + responseOf.note, false);
        }
        this.executor.run(VehicleActions.AC_TEMP_SET, TempControl.bodiesOf(normalize, this.tempZone), resultCallback());
    }

    private void renderTempValue() {
        this.tvTempValue.setText(TempControl.format(this.pendingTemp, this.fahrenheit));
        this.btnTempUp.setEnabled(!TempControl.atMax(this.pendingTemp));
        this.btnTempDown.setEnabled(!TempControl.atMin(this.pendingTemp));
        this.btnTempUp.setAlpha(this.btnTempUp.isEnabled() ? 1.0f : 0.4f);
        this.btnTempDown.setAlpha(this.btnTempDown.isEnabled() ? 1.0f : 0.4f);
    }

    private void renderTemp(VehicleStatus vehicleStatus) {
        boolean platformSupportsHvac = HvacGuard.platformSupportsHvac(this.repo.platformOf(this.repo.selectedVin()));
        this.btnTempApply.setEnabled(platformSupportsHvac);
        this.btnTempApply.setAlpha(platformSupportsHvac ? 1.0f : 0.4f);
        this.tvTempZone.setEnabled(platformSupportsHvac);
        this.btnTempUp.setEnabled(platformSupportsHvac && !TempControl.atMax(this.pendingTemp));
        this.btnTempDown.setEnabled(platformSupportsHvac && !TempControl.atMin(this.pendingTemp));
        if (vehicleStatus == null) {
            this.tvTempHint.setText("车况未同步，下发后无法确认结果");
            this.tvTempHint.setTextColor(UiCompat.getColor(this, R.color.text_hint));
        } else if (vehicleStatus.acTempSet != null) {
            this.tvTempHint.setText("当前设定 " + TempControl.format(vehicleStatus.acTempSet.floatValue(), this.fahrenheit));
            this.tvTempHint.setTextColor(UiCompat.getColor(this, R.color.state_ok));
        } else {
            this.tvTempHint.setText("该车型不回读设定温度，下发后无法确认是否生效");
            this.tvTempHint.setTextColor(UiCompat.getColor(this, R.color.text_hint));
        }
        renderTempValue();
    }

    private void setPollEnabled(boolean z) {
        getSharedPreferences(PREFS, 0).edit().putBoolean(KEY_POLL_ON, z).apply();
        this.repo.setPollEnabled(z);
        if (z) {
            requestNotifPermissionThenStart();
        } else {
            VehiclePollService.stop(this);
            addCard("已停止后台监控，不再推送状态提醒", false);
        }
        applyPollState(z);
    }

    private void requestNotifPermissionThenStart() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIFICATION);
        }
        startMonitoring();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startMonitoring() {
        VehiclePollService.start(this);
        addCard("已开启后台监控：车况变化会在这里提醒并推送通知", false);
    }

    private void applyPollState(boolean z) {
        this.btnPollToggle.setText(z ? "停止监控" : "开启监控");
        int color = UiCompat.getColor(this, z ? R.color.voice_listen : R.color.voice_idle);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(1);
        gradientDrawable.setColor(color);
        gradientDrawable.setStroke(dp(2), color);
        this.alertDot.setBackground(gradientDrawable);
        this.tvAlertHint.setText(z ? "正在监控车况变化，最新动态见下方" : "开启后自动监控车况变化并推送通知");
    }

    @Override // com.dorocar.car.vehicle.StatusAlertCenter.UiSink
    public void onStatusAlert(final StatusAlertCenter.Alert alert) {
        runOnUiThread(new Runnable() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m29lambda$onStatusAlert$25$comdorocarcaruiMainActivity(alert);
            }
        });
    }

    /* renamed from: lambda$onStatusAlert$25$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m29lambda$onStatusAlert$25$comdorocarcaruiMainActivity(StatusAlertCenter.Alert alert) {
        addCard(alert.text, alert.good);
        updateUnreadTitle();
    }

    private void updateUnreadTitle() {
        int unreadCount = StatusAlertCenter.get().unreadCount();
        this.tvAlertTitle.setText(unreadCount > 0 ? "状态提醒 (" + unreadCount + ")" : "状态提醒");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addCard(String str, boolean z) {
        if (str == null || str.isEmpty()) {
            return;
        }
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(13.5f);
        textView.setLineSpacing(dp(3), 1.0f);
        textView.setPadding(dp(12), dp(9), dp(12), dp(9));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(0);
        gradientDrawable.setCornerRadius(dp(14));
        gradientDrawable.setColor(UiCompat.getColor(this, R.color.voice_bubble_sys));
        int dp = dp(1);
        int i = R.color.state_ok;
        gradientDrawable.setStroke(dp, UiCompat.getColor(this, z ? R.color.state_ok : R.color.card_stroke));
        if (!z) {
            i = R.color.text_primary;
        }
        textView.setTextColor(UiCompat.getColor(this, i));
        textView.setBackground(gradientDrawable);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(8);
        textView.setLayoutParams(layoutParams);
        this.dialogContainer.addView(textView);
        while (this.dialogContainer.getChildCount() > 50) {
            this.dialogContainer.removeViewAt(0);
        }
        this.scrollDialog.post(new Runnable() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m0lambda$addCard$26$comdorocarcaruiMainActivity();
            }
        });
    }

    /* renamed from: lambda$addCard$26$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m0lambda$addCard$26$comdorocarcaruiMainActivity() {
        this.scrollDialog.fullScroll(130);
    }

    private void run(String str) {
        addCard("（手动）" + VehicleActions.label(str), false);
        this.executor.run(str, null, resultCallback());
    }

    /* renamed from: com.dorocar.car.ui.MainActivity$1, reason: invalid class name */
    class AnonymousClass1 implements CommandExecutor.Callback {
        AnonymousClass1() {
        }

        /* renamed from: lambda$onProgress$0$com-dorocar-car-ui-MainActivity$1, reason: not valid java name */
        /* synthetic */ void m31lambda$onProgress$0$comdorocarcaruiMainActivity$1(String str) {
            MainActivity.this.addCard("· " + str, false);
        }

        @Override // com.dorocar.car.vehicle.CommandExecutor.Callback
        public void onProgress(final String str) {
            MainActivity.this.runOnUiThread(new Runnable() { // from class: com.dorocar.car.ui.MainActivity$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.AnonymousClass1.this.m31lambda$onProgress$0$comdorocarcaruiMainActivity$1(str);
                }
            });
        }

        @Override // com.dorocar.car.vehicle.CommandExecutor.Callback
        public void onResult(final CommandExecutor.Result result) {
            MainActivity.this.runOnUiThread(new Runnable() { // from class: com.dorocar.car.ui.MainActivity$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.AnonymousClass1.this.m32lambda$onResult$1$comdorocarcaruiMainActivity$1(result);
                }
            });
        }

        /* renamed from: lambda$onResult$1$com-dorocar-car-ui-MainActivity$1, reason: not valid java name */
        /* synthetic */ void m32lambda$onResult$1$comdorocarcaruiMainActivity$1(CommandExecutor.Result result) {
            MainActivity.this.addCard(result.message, result.success);
            if (result.verified) {
                MainActivity.this.repo.refreshNow();
            }
        }
    }

    private CommandExecutor.Callback resultCallback() {
        return new AnonymousClass1();
    }

    private void showHelp() {
        addCard("Doro 车控说明\n\n状态提醒：登录后开启监控，后台每 60 秒同步一次车况，\n以下变化会推送通知并在下方列卡片：\n· 门锁（闭锁/解锁）\n· 车窗/天窗（开/关）\n· 后备箱（开/关）\n· 发动机（启动/熄火）\n· 空调（开/关）\n· 电量跌破 30%/20%/10%\n· 续航不足 100/50 km\n\n远程启动：启动前会检查车门是否关好、电量是否充足；\n可选用 10/15/20/30 分钟运行时长，到点或熄火后自动停机。\n\n空调控制：车辆启动后才能开空调、除霜、方向盘加热，\n熄火状态下这些按钮会变暗并有说明。关空调不受此限制。\n结果同样以车况里空调字段是否变化为准。\n\n下方按钮是手动远控，执行结果也会出现在提醒流里。", false);
    }

    private void sendCode() {
        String trim = this.etPhone.getText().toString().trim();
        if (trim.length() != 11) {
            toast("请输入 11 位手机号");
            return;
        }
        this.btnSendCode.setEnabled(false);
        this.btnSendCode.setText("发送中…");
        this.repo.requestSmsCode(trim, new GwmRepository.Callback<Void>() { // from class: com.dorocar.car.ui.MainActivity.2
            @Override // com.dorocar.car.vehicle.GwmRepository.Callback
            public void onSuccess(Void r2) {
                MainActivity.this.toast("验证码已发送，请查收");
                MainActivity.this.startCodeCountdown();
            }

            @Override // com.dorocar.car.vehicle.GwmRepository.Callback
            public void onError(String str) {
                MainActivity.this.btnSendCode.setEnabled(true);
                MainActivity.this.btnSendCode.setText("获取验证码");
                MainActivity.this.showLoginStatus("发送失败：" + str, true);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Type inference failed for: r0v2, types: [com.dorocar.car.ui.MainActivity$3] */
    public void startCodeCountdown() {
        if (this.codeTimer != null) {
            this.codeTimer.cancel();
        }
        this.btnSendCode.setEnabled(false);
        this.codeTimer = new CountDownTimer(60000L, 1000L) { // from class: com.dorocar.car.ui.MainActivity.3
            @Override // android.os.CountDownTimer
            public void onTick(long j) {
                MainActivity.this.btnSendCode.setText((j / 1000) + " 秒后重发");
            }

            @Override // android.os.CountDownTimer
            public void onFinish() {
                MainActivity.this.btnSendCode.setEnabled(true);
                MainActivity.this.btnSendCode.setText("获取验证码");
            }
        }.start();
    }

    private void doLogin() {
        String trim = this.etPhone.getText().toString().trim();
        String obj = this.etPassword == null ? "" : this.etPassword.getText().toString();
        String trim2 = this.etCode.getText().toString().trim();
        if (trim.length() != 11) {
            toast("请输入 11 位手机号");
            return;
        }
        this.btnLogin.setEnabled(false);
        this.btnLogin.setText("登录中…");
        GwmRepository.Callback<List<GwmVehicle>> callback = new GwmRepository.Callback<List<GwmVehicle>>() { // from class: com.dorocar.car.ui.MainActivity.4
            @Override // com.dorocar.car.vehicle.GwmRepository.Callback
            public void onSuccess(List<GwmVehicle> list) {
                MainActivity.this.btnLogin.setEnabled(true);
                MainActivity.this.btnLogin.setText("登录");
                MainActivity.this.showLoginStatus("登录成功", false);
                MainActivity.this.toast("登录成功");
                if (MainActivity.this.getSharedPreferences(MainActivity.PREFS, 0).getBoolean(MainActivity.KEY_POLL_ON, false)) {
                    MainActivity.this.startMonitoring();
                }
            }

            @Override // com.dorocar.car.vehicle.GwmRepository.Callback
            public void onError(String str) {
                MainActivity.this.btnLogin.setEnabled(true);
                MainActivity.this.btnLogin.setText("登录");
                MainActivity.this.showLoginStatus("登录失败：" + str, true);
            }
        };
        if (!obj.isEmpty()) {
            this.repo.loginPassword(trim, obj, callback);
        } else {
            if (trim2.isEmpty()) {
                this.btnLogin.setEnabled(true);
                this.btnLogin.setText("登录");
                toast("请填写验证码，或输入密码登录");
                return;
            }
            this.repo.loginSms(trim, trim2, callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showLoginStatus(String str, boolean z) {
        this.tvLoginStatus.setVisibility(0);
        this.tvLoginStatus.setText(str);
        this.tvLoginStatus.setTextColor(UiCompat.getColor(this, z ? R.color.state_err : R.color.state_ok));
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this).setTitle("退出登录").setMessage("退出后将停止状态提醒与远控，需重新用短信验证码登录。").setNegativeButton("取消", (DialogInterface.OnClickListener) null).setPositiveButton("退出", new DialogInterface.OnClickListener() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda30
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m26lambda$confirmLogout$27$comdorocarcaruiMainActivity(dialogInterface, i);
            }
        }).show();
    }

    /* renamed from: lambda$confirmLogout$27$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m26lambda$confirmLogout$27$comdorocarcaruiMainActivity(DialogInterface dialogInterface, int i) {
        getSharedPreferences(PREFS, 0).edit().putBoolean(KEY_POLL_ON, false).apply();
        VehiclePollService.stop(this);
        applyPollState(false);
        this.repo.logout();
        addCard("已退出登录", false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toast(String str) {
        Toast.makeText(this, str, 0).show();
    }

    private static void setAlpha(View view, boolean z) {
        view.setAlpha(z ? 1.0f : 0.4f);
    }

    private String effectivePlatform(GwmVehicle gwmVehicle) {
        if (gwmVehicle == null) {
            return null;
        }
        String platformOf = this.repo.platformOf(gwmVehicle.vin);
        if (platformOf == null || platformOf.isEmpty()) {
            if (gwmVehicle.platform == null || gwmVehicle.platform.isEmpty()) {
                return null;
            }
            return gwmVehicle.platform;
        }
        return platformOf;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: renderLoginState, reason: merged with bridge method [inline-methods] */
    public void m27lambda$onLoginStateChanged$28$comdorocarcaruiMainActivity(boolean z) {
        if (z) {
            this.loginBox.setVisibility(8);
            this.loginDivider.setVisibility(0);
            this.controlBox.setVisibility(0);
            GwmVehicle selectedVehicle = this.repo.selectedVehicle();
            this.tvVehicleName.setText(selectedVehicle == null ? "已登录" : selectedVehicle.subtitleWithPlatform(effectivePlatform(selectedVehicle)));
            this.repo.refreshNow();
            return;
        }
        GwmRepository.SessionState sessionState = this.repo.sessionState();
        if (sessionState != GwmRepository.SessionState.RESTORING) {
            this.loginBox.setVisibility(0);
            this.loginDivider.setVisibility(0);
            this.controlBox.setVisibility(8);
            this.tvVehicleName.setText("未登录");
            this.tvLock.setText("--");
            this.tvBattery.setText("--");
            this.tvRange.setText("--");
            this.tvTemp.setText("--");
            renderHvac(null);
            renderEngine(null);
            renderTemp(null);
            if (sessionState == GwmRepository.SessionState.EXPIRED) {
                String sessionMessage = this.repo.sessionMessage();
                if (sessionMessage == null) {
                    sessionMessage = "登录已过期，请重新验证";
                }
                showLoginStatus(sessionMessage, true);
                // 本地仍留有账号时，默认把手机号填回去，用户只需补一个验证码
                if (this.repo.hasAccount() && this.etPhone.getText().length() == 0) {
                    this.etPhone.setText(this.repo.phone());
                }
                return;
            }
            if (this.repo.hasAccount() && this.etPhone.getText().length() == 0) {
                this.etPhone.setText(this.repo.phone());
            }
            return;
        }
        this.loginBox.setVisibility(8);
        this.loginDivider.setVisibility(8);
        this.controlBox.setVisibility(8);
        this.tvVehicleName.setText("正在恢复登录…");
    }

    @Override // com.dorocar.car.vehicle.GwmRepository.Listener
    public void onLoginStateChanged(final boolean z) {
        runOnUiThread(new Runnable() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda29
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m27lambda$onLoginStateChanged$28$comdorocarcaruiMainActivity(z);
            }
        });
    }

    @Override // com.dorocar.car.vehicle.GwmRepository.Listener
    public void onStatusUpdated(final VehicleStatus vehicleStatus, final String str) {
        runOnUiThread(new Runnable() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m30lambda$onStatusUpdated$29$comdorocarcaruiMainActivity(vehicleStatus, str);
            }
        });
    }

    /* renamed from: lambda$onStatusUpdated$29$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m30lambda$onStatusUpdated$29$comdorocarcaruiMainActivity(VehicleStatus vehicleStatus, String str) {
        String str2;
        if (vehicleStatus == null) {
            if (str != null) {
                this.tvVehicleName.setText("车况读取失败");
            }
            renderHvac(null);
            renderTemp(null);
            return;
        }
        GwmVehicle selectedVehicle = this.repo.selectedVehicle();
        this.tvVehicleName.setText(selectedVehicle == null ? "已登录" : selectedVehicle.subtitleWithPlatform(effectivePlatform(selectedVehicle)));
        this.tvLock.setText(vehicleStatus.doorsLocked == null ? "--" : vehicleStatus.doorsLocked.booleanValue() ? "已锁" : "未锁");
        this.tvLock.setTextColor(UiCompat.getColor(this, Boolean.TRUE.equals(vehicleStatus.doorsLocked) ? R.color.state_ok : R.color.state_warn));
        if (vehicleStatus.batteryPercent != null) {
            str2 = Math.round(vehicleStatus.batteryPercent.doubleValue()) + "%";
        } else if (vehicleStatus.fuelGauge != null && !vehicleStatus.fuelGauge.isEmpty()) {
            str2 = vehicleStatus.fuelGauge + "/8";
        } else {
            str2 = "--";
        }
        this.tvBattery.setText(str2);
        this.tvRange.setText(vehicleStatus.fuelRange == null ? "--" : Math.round(vehicleStatus.fuelRange.doubleValue()) + "km");
        this.tvTemp.setText(vehicleStatus.cabinTemp != null ? String.format(Locale.ROOT, "%.1f°", vehicleStatus.cabinTemp) : "--");
        renderHvac(vehicleStatus);
        renderEngine(vehicleStatus);
        renderTemp(vehicleStatus);
        if (vehicleStatus.acTempSet != null) {
            this.pendingTemp = TempControl.normalize(vehicleStatus.acTempSet.floatValue());
            renderTempValue();
        }
    }

    private void renderEngine(VehicleStatus vehicleStatus) {
        int i = R.color.text_hint;
        if (vehicleStatus == null) {
            this.tvEngineState.setText("--");
            this.tvEngineState.setTextColor(UiCompat.getColor(this, R.color.text_hint));
            this.tvEngineHint.setText("车况未同步，无法判断启动状态");
            this.btnEngineStart.setEnabled(true);
            this.btnEngineStop.setEnabled(true);
            return;
        }
        boolean isEngineRunning = vehicleStatus.isEngineRunning();
        this.tvEngineState.setText("● " + vehicleStatus.engineText());
        TextView textView = this.tvEngineState;
        if (isEngineRunning) {
            i = R.color.state_ok;
        }
        textView.setTextColor(UiCompat.getColor(this, i));
        this.tvEngineHint.setText(EngineGuard.hint(vehicleStatus));
        this.btnEngineStart.setEnabled(!isEngineRunning);
        this.btnEngineStop.setEnabled(isEngineRunning);
    }

    private void renderHvac(VehicleStatus vehicleStatus) {
        String platformOf = this.repo.platformOf(this.repo.selectedVin());
        boolean platformSupportsHvac = HvacGuard.platformSupportsHvac(platformOf);
        boolean platformSupportsAux = HvacGuard.platformSupportsAux(platformOf);
        this.btnAcOn.setEnabled(platformSupportsHvac);
        this.btnAcOff.setEnabled(platformSupportsHvac);
        this.btnDefrostFront.setEnabled(platformSupportsHvac && platformSupportsAux);
        this.btnDefrostRear.setEnabled(platformSupportsHvac && platformSupportsAux);
        this.btnSteerHeat.setEnabled(platformSupportsHvac && platformSupportsAux);
        setAlpha(this.btnDefrostFront, this.btnDefrostFront.isEnabled());
        setAlpha(this.btnDefrostRear, this.btnDefrostRear.isEnabled());
        setAlpha(this.btnSteerHeat, this.btnSteerHeat.isEnabled());
        int i = R.color.state_ok;
        if (!platformSupportsAux) {
            this.tvAcHint.setText(HvacGuard.unsupportedPlatformText(platformOf));
            this.tvAcHint.setTextColor(UiCompat.getColor(this, R.color.state_warn));
        } else {
            this.tvAcHint.setText(HvacGuard.hint(vehicleStatus));
            this.tvAcHint.setTextColor(UiCompat.getColor(this, R.color.state_ok));
        }
        String str = "--";
        if (vehicleStatus == null) {
            this.tvAcState.setText("--");
            this.tvAcState.setTextColor(UiCompat.getColor(this, R.color.text_hint));
            this.tvAcCabin.setText("车内温度 --");
            return;
        }
        Boolean bool = vehicleStatus.airConditioner;
        if (bool == null) {
            this.tvAcState.setText("该车型无空调状态");
            this.tvAcState.setTextColor(UiCompat.getColor(this, R.color.text_hint));
        } else {
            this.tvAcState.setText(bool.booleanValue() ? "● 空调运行中" : "○ 空调已关闭");
            TextView textView = this.tvAcState;
            if (!bool.booleanValue()) {
                i = R.color.text_hint;
            }
            textView.setTextColor(UiCompat.getColor(this, i));
        }
        StringBuilder sb = new StringBuilder("车内温度 ");
        if (vehicleStatus.cabinTemp != null) {
            str = String.format(Locale.ROOT, "%.1f°C", vehicleStatus.cabinTemp);
        }
        sb.append(str);
        if (Boolean.TRUE.equals(vehicleStatus.acAutoMode)) {
            sb.append(" · AUTO");
        }
        if (Boolean.TRUE.equals(vehicleStatus.frontDefroster)) {
            sb.append(" · 前除霜");
        }
        if (Boolean.TRUE.equals(vehicleStatus.rearDefroster)) {
            sb.append(" · 后除霜");
        }
        if (Boolean.TRUE.equals(vehicleStatus.steerWheelHeat)) {
            sb.append(" · 方向盘加热");
        }
        this.tvAcCabin.setText(sb.toString());
    }

    @Override // com.dorocar.car.vehicle.GwmRepository.Listener
    public void onRefreshing(boolean z) {
    }

    /* renamed from: lambda$onReauthRequired$30$com-dorocar-car-ui-MainActivity, reason: not valid java name */
    /* synthetic */ void m28lambda$onReauthRequired$30$comdorocarcaruiMainActivity(String str) {
        addCard("登录已失效：" + str, false);
    }

    @Override // com.dorocar.car.vehicle.GwmRepository.Listener
    public void onReauthRequired(final String str) {
        runOnUiThread(new Runnable() { // from class: com.dorocar.car.ui.MainActivity$$ExternalSyntheticLambda28
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m28lambda$onReauthRequired$30$comdorocarcaruiMainActivity(str);
            }
        });
    }

    private int dp(int i) {
        return (int) ((i * getResources().getDisplayMetrics().density) + 0.5f);
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        StatusAlertCenter.get().clearUnread();
        updateUnreadTitle();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        // 从后台被系统回收后重新进入：界面状态以仓库层的真实会话为准，而不是保留旧的"未登录"视图
        if (this.repo.isLoggedIn()) {
            m27lambda$onLoginStateChanged$28$comdorocarcaruiMainActivity(true);
        } else if (this.repo.sessionState() == GwmRepository.SessionState.RESTORING) {
            m27lambda$onLoginStateChanged$28$comdorocarcaruiMainActivity(false);
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        // 只有真正结束（非配置变更/非被系统回收重建）时，才断开监听
        if (isFinishing()) {
            StatusAlertCenter.get().setUiSink(null);
            this.repo.removeListener(this);
        }
        this.main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
