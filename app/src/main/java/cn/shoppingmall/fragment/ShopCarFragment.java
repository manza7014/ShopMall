package cn.shoppingmall.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jude.easyrecyclerview.EasyRecyclerView;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Unbinder;
import cn.shoppingmall.MyApplication;
import cn.shoppingmall.R;
import cn.shoppingmall.activity.ShopCarDeatil;
import cn.shoppingmall.adapter.ShoppingDataAdapter;
import cn.shoppingmall.bean.DataEntity;
import cn.shoppingmall.bean.ShopCarBean;
import cn.shoppingmall.greenDao.GreenDaoUtlis;
import cn.shoppingmall.http.RetrofitHttp;
import cn.shoppingmall.utils.NetUitls;
import cn.shoppingmall.utils.ToastUtils;
import cn.shoppingmall.viewHolder.ShoppingDataViewHolder;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.R.attr.id;
import static cn.shoppingmall.R.id.check_all;
import static cn.shoppingmall.R.id.mRecyclerView;
import static cn.shoppingmall.R.id.shopping_spend;

public class ShopCarFragment extends BaseFragment {
    @BindView(R.id.shopping)
    TextView shopping;

    static TextView shopping_toal_data;
    @BindView(R.id.shop_head_action)
    LinearLayout shop_head_action;

    private static  EasyRecyclerView recyclerView;
    @BindView(R.id.shop_content)
    LinearLayout shopContent;


    @BindView(R.id.shop_total)
    TextView shop_total;
    @BindView(R.id.shopping_pay)
    LinearLayout shopping_pay;
    @BindView(R.id.shopping_calculate_layout)
    LinearLayout shopping_calculate_layout;
    @BindView(R.id.shop_end_action)
    LinearLayout action_layout;
    private static   TextView shopping_spend;
    private static TextView shopping_data_count_sum;
    @BindView(R.id.tv_freight)
    TextView tv_freight;
    @BindView(R.id.shopping_edit)
    TextView shopping_edit;
    public static ShopCarFragment object = new ShopCarFragment();


    private static ArrayList<ShopCarBean.DataEntity.ShopCartListEntity> arrayList = new ArrayList<>();

    private static Context mContext;

    private static ShoppingDataAdapter adapter;

    private GridLayoutManager girdLayoutManager;
    public  static  CheckBox checkAll;
    public static String isCheckAll = "0";

    public static boolean isCheckSingle = false;

    private boolean isEditState = false;

    private static int uid;
    private static ShopCarBean carBean;
    private static DataEntity userinfo;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        adapter = new ShoppingDataAdapter(mContext);
    }




    @Override
    public void onResume() {
        if (adapter.getCount() == 0&&adapter!=null) {
            recyclerView.showError();
        }
        super.onResume();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_shop_car;
    }

    @Override
    public void init() {
        checkAll = (CheckBox) view.findViewById(R.id.check_all);
        shopping_toal_data = (TextView) view.findViewById(R.id.shopping_toal_data);
        shopping_data_count_sum = (TextView) view.findViewById(R.id.shopping_data_count_sum);
        shopping_spend = (TextView) view.findViewById(R.id.shopping_spend);
        recyclerView = (EasyRecyclerView) view.findViewById(R.id.shopping_list_data);
        setHasOptionsMenu(true);
        checkAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (carBean!=null){
                        checkAllState();
                        shopping_spend.setText(carBean.getData().getTotalPrice());
                        shopping_data_count_sum.setText(carBean.getData().getTotalNum());
                        String strFreight = carBean.getData().getFreight();
                        Double douFreigh;
                        try{
                            douFreigh = Double.parseDouble(strFreight);
                        }catch (Exception e){
                            douFreigh = 0.0;
                        }
                        if (douFreigh<=0.0){
                            tv_freight.setText("不含运费");
                        }else {
                            tv_freight.setText("￥"+strFreight);
                        }
                    }

                } else if (!isChecked && isCheckSingle) {
                    isCheckSingle = false;
                } else {
                    unCheckAll();
                    shopping_spend.setText(String.valueOf("0"));
                    shopping_data_count_sum .setText(String.valueOf("0"));
                }
            }
        });

    }


    private  void readShopCarList(){
//        UserId (string, optional): *用户名 ,
//                Token (string, optional): *登录凭证 ,
        DataEntity data = new GreenDaoUtlis(getActivity()).query();
        if (data==null){
            return;
        }
        Map<String,String>map = new HashMap<>();
        map.put("UserId",data.getUserId());
        map.put("Token",data.getToken());
        map= NetUitls.getHashMapData(map);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        getShopCarResult(RetrofitHttp.getRetrofit(builder.build()).getShopCar(map));
    }
    private void getShopCarResult(Call<ResponseBody>responseBody){
        responseBody.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Response<ResponseBody> response) {
                try {
                    String result = response.body().string().toString();
                    carBean = MyApplication.gson.fromJson(result,ShopCarBean.class);
                    if (carBean.getData()==null){
                        recyclerView.showError();
                        recyclerView.setErrorView(R.layout.shopping_no_data_error);
                        return;
                    }

                    List<ShopCarBean.DataEntity.ShopCartListEntity>list = carBean.getData().getShopCartList();

                    List<String>numList = new ArrayList<>();
                    if ("true".equals(carBean.getSuccess())){
                        shopping_spend.setText(carBean.getData().getTotalPrice());
                        shopping_data_count_sum .setText(carBean.getData().getTotalNum());
                        adapter = new ShoppingDataAdapter(cxt);
                        for (int i = 0; i < list.size(); i++) {
                            ShopCarBean.DataEntity.ShopCartListEntity entity= list.get(i);
                            String isCheck = entity.getIsCheck();
                            list.get(i).setPid(i);
                            if (isCheck.equals("true")){
                                numList.add(""+i);
                                adapter.setCheckBoolean(i,true);
                            }else {
                                adapter.setCheckBoolean(i,false);
                            }
                        }
                        adapter.addAll(carBean.getData().getShopCartList());
                        if (numList.size()==adapter.getCount()){
                            checkAll.setChecked(true);
                        }
                        recyclerView.setAdapterWithProgress(adapter);
                        if (adapter.getCount() != 0) {
                            girdLayoutManager = new GridLayoutManager(getActivity(), 2);
                            girdLayoutManager.setSpanSizeLookup(adapter.obtainTipSpanSizeLookUp());
                            recyclerView.setLayoutManager(girdLayoutManager);
                            if (action_layout.getVisibility() == View.GONE) {
                                action_layout.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (adapter.getCount() == 0) {
                                recyclerView.showError();
                                action_layout.setVisibility(View.VISIBLE);

                            }
                        }
                        shopping_toal_data.setText("(" + String.valueOf(adapter.getCount()) + ")");
                    }
                    adapter.setOnItemClickListener(new RecyclerArrayAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(int position) {
                            ToastUtils.showToast(""+position);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }
    public void checkAllState() {
        Iterator iterator = adapter.getIsCheckList().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Integer key = (Integer) entry.getKey();
            adapter.setCheckBoolean(key, true);
        }
       specialUpdate();
    }
    public void unCheckAll() {
        Iterator iterator = adapter.getIsCheckList().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Integer key = (Integer) entry.getKey();
            adapter.setCheckBoolean(key, false);
        }
       specialUpdate();
    }
    private static void specialUpdate() {
        Handler handler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                if (adapter!=null)
                adapter.notifyDataSetChanged();
            }
        };
        handler.post(r);
    }

    public static boolean ischeckAllState() {
        Iterator iterator = adapter.getIsCheckList().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            boolean val = (Boolean) entry.getValue();
            if (val != true) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        editActionState();
        userinfo = new GreenDaoUtlis(getActivity()).queryDefult();
        if (userinfo ==null){
            recyclerView.setErrorView(R.layout.shopping_no_data_error);
            return;
        }
        readShopCarList();

    }

    @OnClick({check_all, R.id.shopping_pay,R.id.shopping_edit})
    public void click(View view) {
        switch (view.getId()) {
            case R.id.shopping_edit:
               if (isEditState){
                   editActionState();
               }else {
                   finishActionState();
               }
                break;
            case check_all:
                break;
            case R.id.shopping_pay:
                Intent intent = new Intent(getActivity(), ShopCarDeatil.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("CartBean",carBean.getData());
                intent.putExtra("bundle",bundle);
                startActivity(intent);

//                if (getTotalSum() > 0) {
//                    setUploadOrderData();
//                    Intent intent = new Intent();
////                    intent.putParcelableArrayListExtra("data", arrayList);
////                    intent.setClass(getContext(), ProductPayDetailActivity.class);
//                    startActivity(intent);
//                } else {
//                    Toast.makeText(mContext, "没有选择商品", Toast.LENGTH_SHORT).show();
//                }
                break;
        }
    }

    public void editActionState() {
        ShoppingDataAdapter.setDisplay(false);
        shopping_edit.setText("编辑");
        isEditState = false;
        shopping_calculate_layout.setVisibility(View.VISIBLE);
        specialUpdate();
    }

    public void finishActionState() {
        ShoppingDataAdapter.setDisplay(true);
        shopping_edit.setText("完成");
        isEditState = true;
        shopping_calculate_layout.setVisibility(View.GONE);
        specialUpdate();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        getActivity().getMenuInflater().inflate(R.menu.shopcar_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_shopcar:
                ToastUtils.showToast(item.getTitle().toString());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
//        ShoppingDataAdapter.setDisplay(false);
        isEditState = false;
        super.onPause();
    }
    //删除操作
    public static void deleteOrder(String ID){
//        ID (integer, optional): *要删除的数据行ID ,
//                UserId (string, optional): *用户名 ,
//                Token (string, optional): *登录凭证 ,
//                timestamp (string, optional): *时间戳 ,
//                nonce (string, optional): 随机数 ,
//                signature (string, optional): 加密签名
        Map<String,String>map = new HashMap<>();
        map.put("ID",ID);
        map.put("UserId",userinfo.getUserId());
        map.put("Token",userinfo.getToken());
        map = NetUitls.getHashMapData(map);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        deleteOrderResult(RetrofitHttp.getRetrofit(builder.build()).delShopCart(map),ID);
    }
    private static void deleteOrderResult(Call<ResponseBody>call, final String carid){
    call.enqueue(new Callback<ResponseBody>() {
        @Override
        public void onResponse(Response<ResponseBody> response) {
            try {
                String result = response.body().string().toString();
                ShopCarBean bean = MyApplication.gson.fromJson(result,ShopCarBean.class);

                if ("true".equals(bean.getSuccess())){
                    ToastUtils.showToast(bean.getMsg());
                    List<ShopCarBean.DataEntity.ShopCartListEntity>list = bean.getData().getShopCartList();
                    for (int i = 0; i < list.size(); i++) {
                        ShopCarBean.DataEntity.ShopCartListEntity entity= list.get(i);
                        String isCheck = entity.getIsCheck();
                        list.get(i).setPid(i);
                        if (isCheck.equals("true")){
                            adapter.setCheckBoolean(i,true);
                        }else {
                            adapter.setCheckBoolean(i,false);
                        }
                    }
                    if (adapter.getCount()!=0){
                        adapter.clear();
                        adapter.addAll(list);
                    }
                    shopping_toal_data.setText("(" + String.valueOf(adapter.getCount()) + ")");
                    shopping_spend.setText(bean.getData().getTotalPrice());
                    shopping_data_count_sum .setText(bean.getData().getTotalNum());
                    specialUpdate();
                }else {
                    ToastUtils.showToast(bean.getMsg());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(Throwable t) {

        }
    });
    }
//更新操作的网络请求
    public static void updataShopCar(String ID,String num,String isCheck){
//        UserId (string, optional): *用户名 ,
//                ID (integer, optional): *购物车商品ID ,
//                BuyNum (integer, optional): *商品数量(大于0有效) ,
//                IsCheck (boolean, optional): *选中状态(true选中) ,
//                Token (string, optional): *登录凭证 ,
//                timestamp (string, optional): *时间戳 ,
//                nonce (string, optional): 随机数 ,
//                signature (string, optional): 加密签名
        Map<String,String>map = new HashMap<>();
        map.put("ID",ID);
        map.put("BuyNum",num);
        map.put("IsCheck",isCheck);
        map.put("UserId",userinfo.getUserId());
        map.put("Token",userinfo.getToken());
        map = NetUitls.getHashMapData(map);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        updataShopCarresult(RetrofitHttp.getRetrofit(builder.build()).updateShopCar(map));
    }
    private static void updataShopCarresult(Call<ResponseBody>call){
        call.clone();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Response<ResponseBody> response) {
                try {
                    String result = response.body().string();
                    ShopCarBean bean = MyApplication.gson.fromJson(result,ShopCarBean.class);
                    List<ShopCarBean.DataEntity.ShopCartListEntity> shoplist = bean.getData().getShopCartList();
                    shopping_spend.setText(bean.getData().getTotalPrice());
                    shopping_data_count_sum .setText(bean.getData().getTotalNum());
                    if (adapter.getCount()!=0){
                        adapter.clear();
                        adapter.addAll(shoplist);
                    }
                    for (int i = 0; i < shoplist.size(); i++) {
                        ShopCarBean.DataEntity.ShopCartListEntity entity= shoplist.get(i);
                        String isCheck = entity.getIsCheck();
                        shoplist.get(i).setPid(i);
                        if (isCheck.equals("true")){
                            adapter.setCheckBoolean(i,true);
                        }else {
                            adapter.setCheckBoolean(i,false);
                        }
                    }

                    specialUpdate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }

}
