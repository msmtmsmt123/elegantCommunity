package com.zzc.elegantcommunity.module.news.article;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.zzc.elegantcommunity.ErrorAction;
import com.zzc.elegantcommunity.RetrofitFactory;
import com.zzc.elegantcommunity.api.IMobileNewsApi;
import com.zzc.elegantcommunity.bean.news.MultiNewsArticleBean;
import com.zzc.elegantcommunity.bean.news.MultiNewsArticleDataBean;
import com.zzc.elegantcommunity.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Meiji on 2017/5/18.
 */

public class NewsArticlePresenter implements INewsArticle.Presenter {

    private static final String TAG = "NewsArticlePresenter";
    private INewsArticle.View view;
    private List<MultiNewsArticleDataBean> dataList = new ArrayList<>();
    private String category;
    private String time;
    private Gson gson = new Gson();
    private Random random = new Random();

    public NewsArticlePresenter(INewsArticle.View view) {
        this.view = view;
        this.time = TimeUtil.getCurrentTimeStamp();
    }

//    private int getRandom() {
//        if (this.time != 0) {
//            Random random = new Random();
//            StringBuilder result = new StringBuilder();
//            for (int i = 0; i < 6; i++) {
//                result.append(random.nextInt(10));
//            }
//            return this.time - Integer.parseInt(result.toString());
//        }
//        return 0;
//    }

    @Override
    public void doLoadData(String... category) {

        try {
            if (this.category == null) {
                this.category = category[0];
            }
        } catch (Exception e) {
            ErrorAction.print(e);
        }

        // 释放内存
        if (dataList.size() > 150) {
            dataList.clear();
        }

//        Observable<MultiNewsArticleBean> ob1 = RetrofitFactory.getRetrofit().create(IMobileNewsApi.class)
//                .getNewsArticle(this.category, this.time);
//        Observable<MultiNewsArticleBean> ob2 = RetrofitFactory.getRetrofit().create(IMobileNewsApi.class)
//                .getNewsArticle2(this.category, this.time);

//        Observable.merge(ob1, ob2)

        getRandom()
                .subscribeOn(Schedulers.io())
                .switchMap(new Function<MultiNewsArticleBean, Observable<MultiNewsArticleDataBean>>() {
                    @Override
                    public Observable<MultiNewsArticleDataBean> apply(@NonNull MultiNewsArticleBean multiNewsArticleBean) throws Exception {
                        List<MultiNewsArticleDataBean> dataList = new ArrayList<>();
                        for (MultiNewsArticleBean.DataBean dataBean : multiNewsArticleBean.getData()) {
                            dataList.add(gson.fromJson(dataBean.getContent(), MultiNewsArticleDataBean.class));
                        }
                        return Observable.fromIterable(dataList);
                    }
                })
                .filter(new Predicate<MultiNewsArticleDataBean>() {
                    @Override
                    public boolean test(@NonNull MultiNewsArticleDataBean dataBean) throws Exception {
                        time = dataBean.getBehot_time();
                        if (TextUtils.isEmpty(dataBean.getSource())) {
                            return false;
                        }
                        try {
                            // 过滤头条问答新闻
                            if (dataBean.getSource().contains("头条问答")
                                    || dataBean.getTag().contains("ad")
                                    || dataBean.getSource().contains("悟空问答")) {
                                return false;
                            }
                            // 过滤头条问答新闻
                            if (dataBean.getRead_count() == 0 || TextUtils.isEmpty(dataBean.getMedia_name())) {
                                String title = dataBean.getTitle();
                                if (title.lastIndexOf("？") == title.length() - 1) {
                                    return false;
                                }
                            }
                        } catch (NullPointerException e) {
                            ErrorAction.print(e);
                        }
                        // 过滤重复新闻(与上次刷新的数据比较)
                        for (MultiNewsArticleDataBean bean : dataList) {
                            if (bean.getTitle().equals(dataBean.getTitle())) {
                                return false;
                            }
                        }
                        return true;
                    }
                })
                .toList()
                .map(new Function<List<MultiNewsArticleDataBean>, List<MultiNewsArticleDataBean>>() {
                    @Override
                    public List<MultiNewsArticleDataBean> apply(@NonNull List<MultiNewsArticleDataBean> list) throws Exception {
                        // 过滤重复新闻(与本次刷新的数据比较,因为使用了2个请求,数据会有重复)
                        for (int i = 0; i < list.size() - 1; i++) {
                            for (int j = list.size() - 1; j > i; j--) {
                                if (list.get(j).getTitle().equals(list.get(i).getTitle())) {
                                    list.remove(j);
                                }
                            }
                        }
                        return list;
                    }
                })
                .compose(view.<List<MultiNewsArticleDataBean>>bindToLife())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<MultiNewsArticleDataBean>>() {
                    @Override
                    public void accept(@NonNull List<MultiNewsArticleDataBean> list) throws Exception {
                        if (null != list && list.size() > 0) {
                            doSetAdapter(list);
                        } else {
                            doShowNoMore();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        doShowNetError();
                        ErrorAction.print(throwable);
                    }
                });
    }

    @Override
    public void doLoadMoreData() {
        doLoadData();
    }

    @Override
    public void doSetAdapter(List<MultiNewsArticleDataBean> list) {
        dataList.addAll(list);
        view.onSetAdapter(dataList);
        view.onHideLoading();
    }

    @Override
    public void doRefresh() {
        if (dataList.size() != 0) {
            dataList.clear();
            time = TimeUtil.getCurrentTimeStamp();
        }
        view.onShowLoading();
        doLoadData();
    }

    @Override
    public void doShowNetError() {
        view.onHideLoading();
        view.onShowNetError();
    }

    @Override
    public void doShowNoMore() {
        view.onHideLoading();
        view.onShowNoMore();
    }

    private Observable<MultiNewsArticleBean> getRandom() {

        int i = random.nextInt(10);
        if (i % 2 == 0) {
            Observable<MultiNewsArticleBean> ob1 = RetrofitFactory.getRetrofit().create(IMobileNewsApi.class)
                    .getNewsArticle(this.category, this.time);
            return ob1;
        } else {
            Observable<MultiNewsArticleBean> ob2 = RetrofitFactory.getRetrofit().create(IMobileNewsApi.class)
                    .getNewsArticle2(this.category, this.time);
            return ob2;
        }
    }
}
