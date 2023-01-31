import com.sun.tools.javac.Main;
import duke.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BmpProcessing {

    private static String fileconfig = "Config\\configremote.txt";
    private ConfigMain Conf;

    private FileMain filemain = new FileMain();
    private DayMain dayMain = new DayMain();

    private static String filestation = "station.cfg";
    private String pathachive = "";
    public String charsetname = "Windows-1251";
    public SimpleDateFormat dateformat = new SimpleDateFormat("dd#MM#yyyy#HH#mm");
    private SimpleDateFormat dateformatsmall = new SimpleDateFormat("dd_MM_yyyy");
    private SimpleDateFormat dateformatsql = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private String username;
    private String password;
    private String connectURL;
    private int typesql = 0;

    private ManagementSQL mansql;

    private ArrayList<Station> stationlist = new ArrayList<Station>();

    BmpProcessing () {
        // читаем файл конфигурации
        Conf = new ConfigMain(fileconfig);
        LOGGER.log(Level.INFO, "Load config file:" + fileconfig);

        pathachive = Conf.get("PATHACHIVE");
        connectURL = Conf.get("URLsql");
        username = Conf.get("USER");
        password = Conf.get("PASSWORD");

        mansql = new ManagementSQL( typesql, connectURL, username, password);

        LoadStationList();

    }

    //читаем файл контролируемых станций
    private void LoadStationList() {

        ArrayList<String> buf = new ArrayList<String>();

        filemain.LoadTextFile(buf, filestation, Charset.forName(charsetname));

        for (String s: buf) {
            int pos1, pos2, pos3 = 0;
            pos1 = s.indexOf("|");
            if (pos1 > 0) {
                pos2 = s.indexOf("|", pos1 + 1);
                if (pos2 > 0) {
                    pos3 = s.indexOf("|", pos2 + 1);
                    if (pos3 > 0) {
                        stationlist.add(new Station(s.substring(0, pos1), s.substring(pos1 + 1, pos2),
                                Integer.parseInt(s.substring(pos2 + 1, pos3)), s.substring(pos3+1).contains("Yes"),
                                pathachive));
                        LOGGER.log(Level.INFO, "Load station: " + s);
                    } else {
                        LOGGER.log(Level.WARNING, "Not load station: " + s);
                    }
                } else {
                    LOGGER.log(Level.WARNING, "Not load station: " + s);
                }
            } else {
                LOGGER.log(Level.WARNING, "Not load station: " + s);
            }
        }

    }

    //обрабатываем скриншоты на станции
    public void LoadStationBMP() {

        for (Station st: stationlist) {
            ArrayList<String> buf = new ArrayList<String>();
            if ((filemain.LoadFile(st.pathname, buf, ".bmp")) && (buf.size() > 0)) {
                LOGGER.log(Level.INFO, "Load BMP in station: " + st.name);

//                buf = SortFileTime(buf);
                buf = SortFileName(buf);

                RebuildArchive(buf, st);

                filemain.deleteAllFilesFolder(st.pathname);

            } else {
                LOGGER.log(Level.WARNING, "Not load bmp in path station: " + st.pathname);
            }
        }
    }

    // просматриваем скриншоты
    private void RebuildArchive(ArrayList<String> buf, Station st) {

        for (String s: buf) {

            String login = st.getLogin(s);
            login = login.substring(login.indexOf("#") + 1);
            System.out.println(login);
            String domain = st.getLogin(s);
            domain = domain.substring(0, domain.indexOf("#"));
            Date date = st.getDate(s);
            System.out.println(domain);
            String flagimg = "0";

            if ((st.loginold.compareTo(login) == 0) && (st.dateold != null)) {
//            if ((st.dateold != null)) {
                Date datenew = dayMain.addMinutesToDate(st.dateold, st.frequency);

                if (date != null) {
                    if (date.compareTo(datenew) >= 0) {
                        if (TransformBMP(s, st)) {
                            st.dateold = st.getDate(s);
                            st.loginold = login;
                            flagimg = "1";
                        }
                    }
                } else {
                    LOGGER.log(Level.WARNING, "File name not corrected: " + s);
                }
            } else {
                if (TransformBMP(s, st)) {
                    st.dateold = date;
                    st.loginold = login;
                    flagimg = "1";
                }
            }

            String sqlstring = "insert into ActionPoint (UserLogin,Station,DateTimeAction,Flagimg,UserDomain) values(\'" + login +
                    "\',\'" + st.name + "\',\'" + dateformatsql.format(date) + "\',\'" + flagimg + "\',\'" + domain + "\')";
            System.out.println(sqlstring);
            mansql.InsertSQL(sqlstring);
        }
    }

    // преобразование скриншота
    private boolean TransformBMP(String s, Station st) {

        File f = new File(st.pathname + s);
        String sbuf = s;
        try {
            BufferedImage image = ImageIO.read(f);

            if (image != null) {
                if (!st.flagcontrol) {
                    BufferedImage imagenew = ImageMain.resize(image, Image.SCALE_DEFAULT);
                    image = imagenew;
                }

                String dir = pathachive + st.name;
                File destws = new File(dir);
                if (!destws.exists()) { destws.mkdir(); }

                dir = dir + "\\" + dateformatsmall.format(st.getDate(s));
                File destdate = new File(dir);
                if (!destdate.exists()) { destdate.mkdir(); }

                StringBuffer sb = new StringBuffer(s);
                int in = indexOfPattern( s,"#.#");
                if (in > 0) {
                    sb.insert(in + 1, "0");
                    sbuf = sb.toString();
                }
                File dest =  new File(dir + "\\" + sbuf.substring(0,sbuf.indexOf(".")-2) + "00.png");
                ImageIO.write( image, "PNG", dest);

                return true;
            } else {
                LOGGER.log(Level.WARNING, "File not image: " + s);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "File image not found: " + s);
            LOGGER.log(Level.WARNING, ex.toString());
        }
        return false;
    }

    // сортировать по времени создания
    private ArrayList<String> SortFileTime(ArrayList<String> buf) {

        Collections.sort(buf, new Comparator<String>() {
            public int compare(String o1, String o2) {
                try {
                    o1 = o1.substring(o1.indexOf("#")+1, o1.indexOf("."));
                    o1 = o1.substring(o1.indexOf("#")+1);
                    o2 = o2.substring(o2.indexOf("#")+1, o2.indexOf("."));
                    o2 = o2.substring(o2.indexOf("#")+1);
                    return dateformat.parse(o1).compareTo(dateformat.parse(o2));
                } catch (ParseException ex) {
                    LOGGER.log(Level.WARNING, "Not sort bmp station ");
                    return 0; }
            }
        });

        return buf;
    }

    // сортировать по имени
    private ArrayList<String> SortFileName(ArrayList<String> buf) {

        Collections.sort(buf, new Comparator<String>() {
            public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
            }
        });
        return buf;
    }


    static Logger LOGGER;
    static {
        try (FileInputStream ins = new FileInputStream("Log\\log.config")) {
            LogManager.getLogManager().readConfiguration(ins);
            LOGGER = Logger.getLogger(Main.class.getName());
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    private static int indexOfPattern(String s, String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(s);
        if (m.find()) {
            return m.start();
        } else {
            return -1;
        }
    }

}
