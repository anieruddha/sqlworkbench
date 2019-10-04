function redirect(){
  var url = window.location.href ;
  if (url.indexOf('sql-workbench.net') > -1) {
    window.location = url.replace("sql-workbench.net", "sql-workbench.eu");
  }
}
