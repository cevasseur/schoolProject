package pdl.backend;

public class Image {
  public static long count;
  private Long id;
  private String name;
  private byte[] data;
  private String type;
  private String size;

  public Image(final Long id, final String name, final byte[] data, final String type, final String size) {
    this.id = id;
    this.name = name;
    this.data = data;
    this.type = type;
    this.size = size;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public byte[] getData() {
    return data;
  }

  public String getType() {
    return type;
  }

  public String getSize() {
    return size;
  }
}
