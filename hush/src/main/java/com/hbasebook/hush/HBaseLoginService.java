package com.hbasebook.hush;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.eclipse.jetty.http.security.Credential;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;

import com.hbasebook.hush.table.UserTable;

public class HBaseLoginService extends MappedLoginService {
  private final Log LOG = LogFactory.getLog(HBaseLoginService.class);

  public HBaseLoginService(String name) {
    super();
    setName(name);
  }

  @Override
  protected UserIdentity loadUser(String username) {
    ResourceManager manager = null;
    HTable table = null;
    try {
      manager = ResourceManager.getInstance();
      table = manager.getTable(UserTable.NAME);
      Get get = new Get(Bytes.toBytes(username));
      get.addColumn(UserTable.DATA_FAMILY, UserTable.CREDENTIALS);
      get.addColumn(UserTable.DATA_FAMILY, UserTable.ROLES);

      Result result = table.get(get);
      if (result.isEmpty()) {
        return null;
      }

      String credentials = Bytes.toString(result.getValue(
        UserTable.DATA_FAMILY, UserTable.CREDENTIALS));
      String roles = Bytes.toString(result.getValue(UserTable.DATA_FAMILY,
        UserTable.ROLES));

      return putUser(username, Credential.getCredential(credentials),
        roles.split(","));
    } catch (Exception e) {
      LOG.error(String.format("Unable to get user '%s'", username), e);
      return null;
    } finally {
      try {
        manager.putTable(table);
      } catch (Exception e) {
        // ignore
      }
    }
  }

  @Override
  protected void loadUsers() throws IOException {
    ResourceManager manager = ResourceManager.getInstance();
    HTable table = manager.getTable(UserTable.NAME);

    Scan scan = new Scan();
    scan.addColumn(UserTable.DATA_FAMILY, UserTable.CREDENTIALS);
    scan.addColumn(UserTable.DATA_FAMILY, UserTable.ROLES);
    ResultScanner scanner = table.getScanner(scan);

    Iterator<Result> results = scanner.iterator();
    int errors = 0;
    while (results.hasNext()) {
      Result result = results.next();
      if (!result.isEmpty()) {
        try {
          String username = Bytes.toString(result.getRow());
          String credentials = Bytes.toString(result.getValue(
            UserTable.DATA_FAMILY, UserTable.CREDENTIALS));
          String roles = Bytes.toString(result.getValue(UserTable.DATA_FAMILY,
            UserTable.ROLES));
          putUser(username, Credential.getCredential(credentials), roles
            .split(","));
        } catch (Exception e) {
          errors++;
        }
      }
    }
    if (errors > 0) {
      LOG.error(String.format("Encountered %d errors in loadUser", errors));
    }

    manager.putTable(table);
  }
}
