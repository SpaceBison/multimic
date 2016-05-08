package org.spacebison.multimic.ui;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.spacebison.common.CrashlyticsLog;
import org.spacebison.multimic.R;
import org.spacebison.multimic.model.ClientService;
import org.spacebison.multimic.net.discovery.MulticastServiceResolver;
import org.spacebison.multimic.net.discovery.message.ResolvedService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ServerSearchActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, MulticastServiceResolver.Listener {

    private static final String TAG = "ServerSearchActivity";
    @Bind(R.id.swipe_layout)
    SwipeRefreshLayout mSwipeLayout;
    @Bind(R.id.recycler)
    RecyclerView mRecyclerView;

    private ServiceListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_search);
        ButterKnife.bind(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorSchemeResources(R.color.accent);

        mAdapter = new ServiceListAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSwipeLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeLayout.setRefreshing(true);
                searchForServers();
            }
        });
    }

    private void searchForServers() {
        ClientService.resolveServers(5000, this);
    }

    @Override
    public void onRefresh() {
        searchForServers();
    }

    @Override
    public void onServicesResolved(Set<ResolvedService> services) {
        if (mAdapter != null) {
            mAdapter.setResolvedServices(services);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSwipeLayout != null) {
                    mSwipeLayout.setRefreshing(false);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class ServiceViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.name)
        TextView mName;
        @Bind(R.id.address)
        TextView mAddress;

        public ServiceViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private class ServiceListAdapter extends RecyclerView.Adapter<ServiceViewHolder> {
        private List<ResolvedService> mResolvedServices;

        @Override
        public ServiceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_resolved_service, parent, false);
            return new ServiceViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ServiceViewHolder holder, int position) {
            ResolvedService service = mResolvedServices.get(position);
            CrashlyticsLog.d(TAG, "Service " + position + ": " + service);
            holder.mName.setText(service.name);
            holder.mAddress.setText(service.address.getHostAddress());
        }

        @Override
        public int getItemCount() {
            if (mResolvedServices != null) {
                return mResolvedServices.size();
            } else {
                return 0;
            }
        }

        public void setResolvedServices(Set<ResolvedService> resolvedServices) {
            mResolvedServices = new ArrayList<>(resolvedServices);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }
}
