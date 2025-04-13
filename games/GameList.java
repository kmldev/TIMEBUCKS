package org.mintsoft.mintly.games;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetGame;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;
import org.mintsoft.mintly.helper.htmlAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class GameList extends AppCompatActivity {
    private RecyclerView gridView;
    private Dialog loadingView, conDiag, requireDiag;
    private TextView reqDesc;
    private htmlAdapter adapter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.game_html_gamelist);
        TextView titleView = findViewById(R.id.game_html_gamelist_title);
        titleView.setText(DataParse.getStr(this, "game_list", Home.spf));
        gridView = findViewById(R.id.game_html_hamelist_gridView);
        loadingView = Misc.loadingDiag(this);
        callNet();
        findViewById(R.id.game_html_gamelist_back).setOnClickListener(view -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Variables.getHash("show_offers") != null) finish();
    }

    private void callNet() {
        if (!loadingView.isShowing()) loadingView.show();
        GetGame.getHtml(this, "all", new onResponse() {
            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> list) {
                adapter = new htmlAdapter(GameList.this, list, R.layout.game_html_gl_item, 14);
                GridLayoutManager manager = new GridLayoutManager(GameList.this, 2);
                manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        if (adapter.getItemViewType(position) == htmlAdapter.VIEW_TYPE_ITEM) {
                            return 1;
                        } else {
                            return 2;
                        }
                    }
                });
                gridView.setLayoutManager(manager);
                gridView.setAdapter(adapter);
                adapter.recyclerState(gridView);
                loadingView.dismiss();
            }

            @Override
            public void onError(int errorCode, String error) {
                loadingView.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, GameList.this, () -> {
                        callNet();
                        loadingView.dismiss();
                    });
                } else {
                    Toast.makeText(GameList.this, error, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }
}