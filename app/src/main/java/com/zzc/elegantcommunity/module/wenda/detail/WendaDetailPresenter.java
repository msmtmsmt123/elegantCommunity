package com.zzc.elegantcommunity.module.wenda.detail;


import com.zzc.elegantcommunity.ErrorAction;
import com.zzc.elegantcommunity.RetrofitFactory;
import com.zzc.elegantcommunity.api.IMobileNewsApi;
import com.zzc.elegantcommunity.api.IMobileWendaApi;
import com.zzc.elegantcommunity.bean.news.NewsCommentBean;
import com.zzc.elegantcommunity.util.SettingUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

/**
 * Created by Meiji on 2017/5/23.
 */

public class WendaDetailPresenter implements IWendaDetail.Presenter {

    private IWendaDetail.View view;
    private String groupId;
    private int offset = 0;
    private List<NewsCommentBean.DataBean.CommentBean> commentsBeanList = new ArrayList<>();

    WendaDetailPresenter(IWendaDetail.View view) {
        this.view = view;
    }

    @Override
    public void doRefresh() {
        if (commentsBeanList.size() != 0) {
            commentsBeanList.clear();
            offset = 0;
        }
        view.onLoadData();
    }

    @Override
    public void doLoadData(String url) {
        RetrofitFactory.getRetrofit().create(IMobileWendaApi.class)
                .getWendaAnsDetail(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ResponseBody>() {
                    @Override
                    public void accept(@NonNull ResponseBody responseBody) throws Exception {
                        String result = getHTML(responseBody.string());
                        if (result != null) {
                            view.onSetWebView(result, true);
                        } else {
                            view.onSetWebView(null, false);
                        }
                        view.onHideLoading();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        view.onSetWebView(null, false);
                        view.onHideLoading();
                        ErrorAction.print(throwable);
                    }
                });
    }

    @Override
    public void doLoadComment(String... ansId) {

        try {
            if (null == groupId) {
                this.groupId = ansId[0];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        RetrofitFactory.getRetrofit().create(IMobileNewsApi.class)
                .getNewsComment(groupId, offset)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<NewsCommentBean, List<NewsCommentBean.DataBean.CommentBean>>() {
                    @Override
                    public List<NewsCommentBean.DataBean.CommentBean> apply(@NonNull NewsCommentBean newsCommentBean) throws Exception {
                        List<NewsCommentBean.DataBean.CommentBean> data = new ArrayList<>();
                        for (NewsCommentBean.DataBean bean : newsCommentBean.getData()) {
                            data.add(bean.getComment());
                        }
                        return data;
                    }
                })
                .compose(view.<List<NewsCommentBean.DataBean.CommentBean>>bindToLife())
                .subscribe(new Consumer<List<NewsCommentBean.DataBean.CommentBean>>() {
                    @Override
                    public void accept(@NonNull List<NewsCommentBean.DataBean.CommentBean> list) throws Exception {
                        if (list.size() > 0) {
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
    public void doLoadMoreComment() {
        offset += 10;
        doLoadComment();
    }

    @Override
    public void doSetAdapter(List<NewsCommentBean.DataBean.CommentBean> list) {
        commentsBeanList.addAll(list);
        view.onSetAdapter(commentsBeanList);
        view.onHideLoading();
    }

    @Override
    public void doShowNetError() {
        view.onHideLoading();
        view.onShowNetError();
    }

    @Override
    public void doShowNoMore() {
        view.onHideLoading();
        if (commentsBeanList.size() > 0) {
            view.onShowNoMore();
        }
    }

    private String getHTML(String response) {
        Document doc = Jsoup.parse(response, "UTF-8");
        Elements elements = doc.getElementsByClass("con-words");
        String content = null;
        for (Element element : elements) {
            content = element.toString();
            break;
        }
        if (content != null) {

            String css = "<link rel=\"stylesheet\" href=\"file:///android_asset/toutiao_light.css\" type=\"text/css\">";
            if (SettingUtil.getInstance().getIsNightMode()) {
                css = css.replace("toutiao_light", "toutiao_dark");
            }

            String html = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">" +
                    css +
                    "<body>\n" +
                    "<article class=\"article-container\">\n" +
                    "    <div class=\"article__content article-content\">" +
                    content +
                    "    </div>\n" +
                    "</article>\n" +
                    "</body>\n" +
                    "</html>";

            return html;
        } else {
            return null;
        }
    }
}
