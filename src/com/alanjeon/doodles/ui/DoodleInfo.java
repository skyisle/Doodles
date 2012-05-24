
package com.alanjeon.doodles.ui;

import java.io.Serializable;

public class DoodleInfo implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public String title;
    public String url;
    int height;
    public boolean is_dynamic;
    long start;
    long end;
    public String blog_text;
    public String name;

    public String run_date;
}
