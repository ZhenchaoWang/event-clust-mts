package edu.whu.cs.nlp.mts.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.whu.cs.nlp.mts.domain.Word;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import wzc.util.c3p0.C3P0Util;

/**
 * 缓存框架工具类
 * @author Apache_xiaochao
 *
 */
public class EhCacheUtil {

    private volatile static CacheManager cacheManager;
    public String cacheName = "db_cache_vec";
    public int dimension = 300;  //维度
    public String datasource = "localhost-3306-user_vec";

    public EhCacheUtil(String cacheName, int dimension, String datasource) {
        super();
        this.cacheName = cacheName;
        this.dimension = dimension;
        this.datasource = datasource;
        if(cacheManager == null){
            synchronized (EhCacheUtil.class) {
                if(cacheManager == null){
                    //加载EnCache的配置文件
                    cacheManager = new CacheManager();
                }
            }
        }
    }

    /**
     * 从缓存中获取当前word对应的向量
     * @param word
     * @return
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    public synchronized List<Float[]> getVec(Word word) throws SQLException{
        List<Float[]> vecs = new ArrayList<Float[]>();
        if(word != null){
            //获取指定word对应的词向量
            final Cache cache = cacheManager.getCache(this.cacheName);
            final Element element = cache.get(word.getName().toLowerCase());
            if(element != null){
                //命中
                vecs = (List<Float[]>)element.getObjectValue();
            }else{
                //未命中
                Connection connection = null;
                PreparedStatement ps = null;
                ResultSet rs = null;
                try {
                    final String sql = "SELECT * FROM word2vec WHERE word = ?";
                    connection = C3P0Util.getConnection(this.datasource);
                    List<Float[]> vecs_tmp = new ArrayList<Float[]>();
                    try{
                        ps = connection.prepareStatement(sql);
                        ps.setString(1, word.getName().toLowerCase());
                        rs = ps.executeQuery();
                        while(rs.next()){
                            final String vecStr = rs.getString("vec");
                            if(vecStr != null){
                                final Float[] vecs_f = new Float[this.dimension];
                                final String[] str_vecs_arr = vecStr.split("\\s+");
                                for(int i = 0; i < this.dimension; ++i){
                                    vecs_f[i] = Float.parseFloat(str_vecs_arr[i]);
                                }
                                vecs_tmp.add(vecs_f);
                            }
                        }
                    }finally{
                        if(rs != null){
                            rs.close();
                        }
                        if(ps != null){
                            ps.close();
                        }
                    }
                    if(vecs_tmp.size() > 0){
                        //缓存当前得到的词向量
                        cache.put(new Element(word.getName().toLowerCase(), vecs_tmp));
                        vecs = vecs_tmp;
                    }else{
                        //表示当前词向量模型中没有该词，用该词的原型代替
                        try{
                            ps = connection.prepareStatement(sql);
                            ps.setString(1, word.getLemma().toLowerCase());
                            rs = ps.executeQuery();
                            vecs_tmp = new ArrayList<Float[]>();
                            while(rs.next()){
                                final String vecStr = rs.getString("vec");
                                if(vecStr != null){
                                    //vecs_str.add(vecStr);
                                    final Float[] vecs_f = new Float[this.dimension];
                                    final String[] str_vecs_arr = vecStr.split("\\s+");
                                    for(int i = 0; i < this.dimension; ++i){
                                        vecs_f[i] = Float.parseFloat(str_vecs_arr[i]);
                                    }
                                    vecs_tmp.add(vecs_f);
                                }
                            }
                        }finally{
                            if(rs != null){
                                rs.close();
                            }
                            if(ps != null){
                                ps.close();
                            }
                        }
                        if(vecs_tmp.size() > 0){
                            //缓存当前得到的词向量
                            cache.put(new Element(word.getName().toLowerCase(), vecs_tmp));
                            vecs = vecs_tmp;
                        }else{
                            //还是没有的话，如果当前是命名实体，就用该命名实体的向量来代替
                            if(!"O".equalsIgnoreCase(word.getNer())){
                                try{
                                    ps = connection.prepareStatement(sql);
                                    ps.setString(1, word.getNer().toLowerCase());
                                    rs = ps.executeQuery();
                                    vecs_tmp = new ArrayList<Float[]>();
                                    while(rs.next()){
                                        final String vecStr = rs.getString("vec");
                                        if(vecStr != null){
                                            //vecs_str.add(vecStr);
                                            final Float[] vecs_f = new Float[this.dimension];
                                            final String[] str_vecs_arr = vecStr.split("\\s+");
                                            for(int i = 0; i < this.dimension; ++i){
                                                vecs_f[i] = Float.parseFloat(str_vecs_arr[i]);
                                            }
                                            vecs_tmp.add(vecs_f);
                                        }
                                    }
                                }finally{
                                    if(rs != null){
                                        rs.close();
                                    }
                                    if(ps != null){
                                        ps.close();
                                    }
                                }
                                if(vecs_tmp.size() > 0){
                                    //缓存当前得到的词向量
                                    cache.put(new Element(word.getName().toLowerCase(), vecs_tmp));
                                    vecs = vecs_tmp;
                                }
                            }
                        }
                    }
                } finally{
                    if(connection != null){
                        connection.close();
                    }
                }
            }
        }
        return vecs;
    }

}
