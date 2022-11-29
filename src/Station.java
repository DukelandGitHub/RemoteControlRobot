import duke.FileMain;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Station {

    private FileMain filemain = new FileMain();
    public SimpleDateFormat dateformat = new SimpleDateFormat("dd#MM#yyyy#HH#mm");

    public String name = "";
    public String pathname = "";
    public int frequency = 15;
    public boolean flagcontrol = false;
    public String loginold = "";
    public Date dateold = null;

    Station(String name, String pathname, int frequency, boolean flagcontrol, String patharc) {
        this.name = name;
        this.pathname = pathname;
        this.frequency = frequency;
        this.flagcontrol = flagcontrol;
        LoadOld(patharc);
    }

    private boolean LoadOld(String patharc) {

        ArrayList<String> bufdir = new ArrayList<String>();
        ArrayList<String> buf = new ArrayList<String>();

        if (filemain.LoadDirectory(patharc + this.name, bufdir, "")) {
            for (String d: bufdir) {
                if (filemain.LoadFile(patharc + this.name + "\\" + d, buf, ".jpg") && (buf.size() > 0)) {
                    for (String s : buf) {
                        Date datebuf = getDate(s);
                        if (datebuf != null) {
                            if (dateold != null) {
                                if (datebuf.compareTo(dateold) > 0) {
                                    dateold = datebuf;
                                    loginold = getLogin(s);
                                }
                            } else {
                                dateold = datebuf;
                                loginold = getLogin(s);
                            }
                        }
                    }
                } else {
                    return false;
                }
            }
        } else { return false; }
        return true;
    }

    //выделяем в строке дату
    public Date getDate(String s) {

        int pos = s.indexOf("#", s.indexOf("#") + 1);
        if (pos > 0) {
            try {
                return dateformat.parse(s.substring(pos + 1));
            } catch (ParseException ex) {
            }
        }
        return null;
    }

    //выделяем в строке логин
    public String getLogin(String s) {

        int pos = s.indexOf("#", s.indexOf("#") + 1);
        if (pos > 0) {
            return s.substring(0, pos);
        }
        return null;
    }

}
