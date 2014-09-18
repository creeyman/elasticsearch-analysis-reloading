package com.hichao.elasticsearch.analysis.reloading;

import java.util.ArrayList;
import java.util.List;

public class Reloadables {
    private static final List<Reloadable> RELOADABLES = new ArrayList<Reloadable>();
    
    public static void register(Reloadable reloadable){
        
        RELOADABLES.add(reloadable);
    }
    
    public static List<Reloadable> getReloadables(){
        return RELOADABLES;
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Reloadable> List<T> getReloadables(Class<T> reloadableClass){
        
        List<T> reloadableList = new ArrayList<T>();
        
        for (Reloadable reloadable : RELOADABLES) {
            if (reloadable.getClass() == reloadableClass){
                reloadableList.add((T)reloadable);
            }
        }
        return reloadableList;
    }
    
    public static void reloadAll(){
        for (Reloadable reloadable : RELOADABLES) {
            reloadable.reload();
        }
    }
    
    public static <T extends Reloadable> void reload(Class<T> reloadableClass){
        for (Reloadable reloadable : RELOADABLES) {
            if (reloadable.getClass() == reloadableClass){
                reloadable.reload();;
            }
        }
    }
}
