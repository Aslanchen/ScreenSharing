package aslanchen.screensharing;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements OnItemClickListener, OnClickListener {

    public static final int DISPLAY_STATE_NOT_CONNECTED = 0;
    public static final int DISPLAY_STATE_CONNECTING = 1;
    public static final int DISPLAY_STATE_CONNECTED = 2;

    private Button bt_back, bt_rush;
    private ListView ls_show;

    private List<WifiDisplayPro> listAvilable;
    private List<WifiDisplayPro> listRemember;
    private WifiDisplayAdapter wifiAdapter;
    private int connectIndex = -1;

    private DisplayManager mDisplayManager;

    private int state = 0;// 0：断开 1：正在连接 2：已经连接
    private Method getWifiDisplayStatus;
    private Object mWifiDisplayStatus;

    private Runnable outRunnable;
    private Handler outHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        InitView();
        IniListener();
        InitData();
    }

    private void InitView() {
        bt_back = (Button) findViewById(R.id.bt_back);
        bt_rush = (Button) findViewById(R.id.bt_rush);
        ls_show = (ListView) findViewById(R.id.ls_show);
    }

    private void IniListener() {
        bt_back.setOnClickListener(this);
        bt_rush.setOnClickListener(this);
        ls_show.setOnItemClickListener(this);
    }

    private void InitData() {
        IntentFilter iss = new IntentFilter();
        iss.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
        iss.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        this.registerReceiver(mBroadcastReceiver, new IntentFilter(iss));

        mDisplayManager = (DisplayManager) this.getSystemService(Context.DISPLAY_SERVICE);
        listAvilable = new ArrayList<WifiDisplayPro>();
        listRemember = new ArrayList<WifiDisplayPro>();

        wifiAdapter = new WifiDisplayAdapter(this, listAvilable);
        ls_show.setAdapter(wifiAdapter);

        outHandle = new Handler();
        outRunnable = new Runnable() {

            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "连接超时,请重启连接设备！", Toast.LENGTH_SHORT).show();
            }
        };

        startScan();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.bt_rush) {
            startScan();
        } else if (id == R.id.bt_back) {
            this.finish();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == connectIndex) {
            // 断开
            try {
                Method disconnectWifiDisplay = mDisplayManager.getClass().getMethod("disconnectWifiDisplay");
                disconnectWifiDisplay.invoke(mDisplayManager);
                connectIndex = -1;
                state = DISPLAY_STATE_NOT_CONNECTED;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            // 连接
            try {
                Method connectWifiDisplay = mDisplayManager.getClass().getMethod("connectWifiDisplay", String.class);
                connectWifiDisplay.invoke(mDisplayManager, listAvilable.get(position).getDeviceAddress());
                connectIndex = position;
                state = DISPLAY_STATE_CONNECTING;
                outHandle.postDelayed(outRunnable, 10 * 1000);// 超时检查
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        wifiAdapter.setState(DISPLAY_STATE_CONNECTING);
        wifiAdapter.setConnectIndex(connectIndex);
        wifiAdapter.notifyDataSetChanged();
    }


    private void startScan() {
        try {
            // 反射出扫描方法
            Method scanWifiDisplays = mDisplayManager.getClass().getMethod("scanWifiDisplays");
            // 反射出获取扫描结果方法
            getWifiDisplayStatus = mDisplayManager.getClass().getMethod("getWifiDisplayStatus");

            // 开始扫描
            scanWifiDisplays.invoke(mDisplayManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void getAvivableDrive() {
        listAvilable.clear();
        listRemember.clear();

        try {
            // 获取扫描后的对象
            mWifiDisplayStatus = getWifiDisplayStatus.invoke(mDisplayManager);

            // 获取支持的设备值
            Field mAvailableDisplays = mWifiDisplayStatus.getClass().getDeclaredField("mAvailableDisplays");
            mAvailableDisplays.setAccessible(true);// 设置安全检查，访问私有成员变量必须
            Object mAvailablefildValue = mAvailableDisplays.get(mWifiDisplayStatus);
            Object[] osAva = (Object[]) mAvailablefildValue;

            for (int i = 0; i < osAva.length; i++) {
                Object cur = osAva[i];
                listAvilable.add(getValue(cur));
            }

            // 获取记住的设备
            Field mRememberedDisplays = mWifiDisplayStatus.getClass().getDeclaredField("mRememberedDisplays");
            mRememberedDisplays.setAccessible(true);// 设置安全检查，访问私有成员变量必须
            Object mRememberedfildValue = mRememberedDisplays.get(mWifiDisplayStatus);
            Object[] osRember = (Object[]) mRememberedfildValue;

            for (int i = 0; i < osRember.length; i++) {
                Object cur = osRember[i];
                listRemember.add(getValue(cur));
            }

            // 判断有没有连接的
            getActiveDisplayValue(osAva);
            getActiveDisplayState();

            wifiAdapter.setConnectIndex(connectIndex);
            wifiAdapter.setState(state);
            wifiAdapter.notifyDataSetChanged();

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private void getActiveDisplayValue(Object[] osAva) {
        try {
            // 获取当前连接屏幕
            Field mActiveDisplay = mWifiDisplayStatus.getClass().getDeclaredField("mActiveDisplay");
            mActiveDisplay.setAccessible(true);// 设置安全检查，访问私有成员变量必须
            Object mActiveDisplayValue = mActiveDisplay.get(mWifiDisplayStatus);
            connectIndex = -1;
            if (mActiveDisplayValue != null) {
                for (Object object : osAva) {
                    if (object == mActiveDisplayValue) {
                        break;
                    }
                    connectIndex++;
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void getActiveDisplayState() {
        // 获取当前连接状态
        try {
            Field mActiveDisplayState = mWifiDisplayStatus.getClass().getDeclaredField("mActiveDisplayState");
            mActiveDisplayState.setAccessible(true);// 设置安全检查，访问私有成员变量必须
            Object mActiveDisplayStateValue = mActiveDisplayState.get(mWifiDisplayStatus);
            state = Integer.valueOf(String.valueOf(mActiveDisplayStateValue));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private WifiDisplayPro getValue(Object clas) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        WifiDisplayPro wdp = new WifiDisplayPro();
        String[] names = new String[]{"mDeviceAddress", "mDeviceAlias", "mDeviceName", "mDeviceType", "mGroupCapability"};

        for (int i = 0; i < names.length; i++) {
            Field field = clas.getClass().getDeclaredField(names[i]);
            field.setAccessible(true);// 设置安全检查，访问私有成员变量必须
            Object object = field.get(clas);

            if (object == null) {
                continue;
            }

            if (i == 0) {
                wdp.setDeviceAddress(String.valueOf(object));
            } else if (i == 1) {
                wdp.setDeviceAlias(String.valueOf(object));
            } else if (i == 2) {
                wdp.setDeviceName(String.valueOf(object));
            } else if (i == 3) {
                wdp.setDeviceType(String.valueOf(object));
            } else if (i == 4) {
                wdp.setGroupCapability(String.valueOf(object));
            }
        }
        return wdp;
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action) == true) {
                if (getWifiDisplayStatus != null) {
                    System.out.println("获取可用设备");
                    getAvivableDrive();
                }
            } else if ("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED".equals(action) == true) {
                // 连接上，断开会触发这个
                if (mWifiDisplayStatus != null) {
                    System.out.println("连接或者断开");
                    if (state == DISPLAY_STATE_CONNECTING) {
                        outHandle.removeCallbacks(outRunnable);
                        state = DISPLAY_STATE_CONNECTED;
                        wifiAdapter.setState(state);
                        wifiAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

}
