package aslanchen.screensharing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class WifiDisplayAdapter extends BaseAdapter {

    private List<WifiDisplayPro> listAvilable;
    private LayoutInflater inflater;
    private int connectIndex = -1;
    private int state = 0;// 0：断开 1：正在连接 2：已经连接

    public WifiDisplayAdapter(Context context, List<WifiDisplayPro> listAvilable) {
        inflater = LayoutInflater.from(context);
        this.listAvilable = listAvilable;
    }

    public int getConnectIndex() {
        return connectIndex;
    }

    public void setConnectIndex(int connectIndex) {
        this.connectIndex = connectIndex;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public static class ViewHolder {
        TextView tv_name, tv_state;
    }

    @Override
    public int getCount() {
        return listAvilable.size();
    }

    @Override
    public Object getItem(int position) {
        return listAvilable.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item, null);
            holder = new ViewHolder();
            holder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
            holder.tv_state = (TextView) convertView
                    .findViewById(R.id.tv_state);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        WifiDisplayPro wp = listAvilable.get(position);
        holder.tv_name.setText(wp.getDeviceName());
        holder.tv_state.setText("可用");

        if (connectIndex == position) {
            if (state == MainActivity.DISPLAY_STATE_NOT_CONNECTED) {
                holder.tv_state.setText("未连接");
            } else if (state == MainActivity.DISPLAY_STATE_CONNECTING) {
                holder.tv_state.setText("正在连接");
            } else if (state == MainActivity.DISPLAY_STATE_CONNECTED) {
                holder.tv_state.setText("已经连接");
            }
        }
        return convertView;
    }

}
