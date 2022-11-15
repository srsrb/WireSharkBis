import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Infos {
  private final String type;
  private Map<String, String> hashInfos = null;

  private String[] header = null; // TOUT sauf header internet
  private List<Character> headerHttpClair = null;

  private final String formatEthernet_fields = "macDst macSrc next",
      // https://en.wikipedia.org/wiki/Ethernet_frame
      formatIpv4_fields = "version lengthHeader ToS lengthTotal id flag offset ttl protocol checksum ipSrc ipDst", // ignoring:
                                                                                                                   // options
                                                                                                                   // and
                                                                                                                   // padding
      // https://fr.wikipedia.org/wiki/IPv4
      formatTcp_fields = " portSrc portDst sequenceNumber ackNumber offset reserved flags",
      // https://en.wikipedia.org/wiki/Transmission_Control_Protocol
      formatHttp_fields = "messageEnClair";

  private final String formatEthernet_nbOctets = "48 48 16",
      formatIpv4_nbOctets = "4 4 8 16 16 3 13 8 8 16 32 32", // ignore_remaning = ignore remaining bits (padding);
      formatTcp_nbOctets = "16 16 32 32 4 3";

  /**
   * 
   * @param type
   * @param header
   */
  public Infos(String type, String[] header) {
    this.type = type;
    this.header = header;

    init();
  }

  private void init() {
    switch (type) {
      case "ethernet":
        hashEthernet();
        break;

      case "ipv4":
        hashIpv4();
        break;

      case "tcp":
        hashTcp();
        break;
      case "http":
        hashHttp();
        break;

      default:
        throw new InvalidParameterException("Type d'entete incorrect: " + type);

    }
  }

  /**
   * !!!! A refaire avec string buider
   * 
   * @param ls
   * @param i,j
   * @return The substring composed of the consecutives characters between the
   *         i-th and the j-th
   */

  private String getSubString(String[] ls, int i, int j) {
    String s = "";
    int i_oct = 0;
    outer: for (int index = 0; index < ls.length; index++) {
      for (int k = 0; k < ls[index].length(); k++) {
        if (i_oct > j) {
          break outer;
        } else if (i <= i_oct) {
          s += ls[index].charAt(k);
        }
        i_oct++;
      }
    }
    return s;
  }

  /**
   * 
   * @param fields   ( formatEthernet_fields, ...)
   * @param nbOctets ( formatEthernet_nbOctets, ...)
   * @return HashTable with all fields filled out (based on this.header and the
   *         String f
   *         )
   */
  private Map<String, String> makeHash(String fields, String nbOctets) {

    String[] lsFields = fields.split(" ");
    String[] lsNb = nbOctets.split(" ");
    if (lsFields.length != lsNb.length) {
      throw new InvalidParameterException("lsNb et lsFields de tailles differentes");
    }
    Map<String, String> hash = new Hashtable<>();

    int n = lsFields.length;

    int octetCourant = 0;

    for (int i = 0; i < n; i++) {
      int octetSuivant = Integer.parseInt(lsNb[i]) + octetCourant;
      String data = getSubString(lsFields, octetCourant, octetSuivant - 1);
      hash.put(lsFields[i], data);
    }
    return hash;
  }

  
  public void hashEthernet() {
    Map<String, String> hash = makeHash(formatEthernet_fields, formatEthernet_nbOctets);
    this.hashInfos = hash;
  }

 
  public void hashIpv4() {
    Map<String, String> hash = makeHash(formatIpv4_fields, formatIpv4_nbOctets);
    this.hashInfos = hash;

  }

 
  public void hashTcp() {
    Map<String, String> hash = makeHash(formatTcp_fields, formatTcp_nbOctets);
    this.hashInfos = hash;

  }

  // !!! a verifier
  private void translateBinaryToAscii() {
    List<Character> ls = new ArrayList<>();
    for (String s : this.header) {
      int charCode = Integer.parseInt(s, 2);
      char c = (char) charCode;
      ls.add(c);
    }
    this.headerHttpClair = ls;
  }

  private int findNewLineChar() throws InvalidParameterException {

    for (int i = 0; i < this.headerHttpClair.size(); i++) {
      if (headerHttpClair.get(i) == '\n') {
        return i;
      }
    }
    throw new InvalidParameterException("Le header Http ne contient pas de '\\n': " + this.headerHttpClair);

  }

  // !!!
  public void hashHttp() {
    translateBinaryToAscii();
    int i = findNewLineChar();
    String s = "";
    for (int k = 0; k < this.headerHttpClair.size(); k++) {
      if (k > i) {
        break;
      }
      s = s + headerHttpClair.get(k);
    }

    this.hashInfos.put(formatHttp_fields, s);

  }


  public void setInfos(String field, String data) {
    this.hashInfos.replace(field, data);
  }


  public Map<String, String> getInfos() {
    return this.hashInfos;
  }


  public String getField(String field) {
    return this.hashInfos.get(field);
  }

  public String getType() {
    return this.type;
  }

  @Override
  public String toString() {
    return "type='" + getType() + "'";
  }
}
