var HideDelay = 1500;

//event
var EventPull = "Pull";
var EventTitle = "Title";
var EventBack = "Back";
var EventBackCallback = "BackCallback";
var EventNormal = "Normal";
var EventNormalCallback = "NormalCallback";
var EventDownload = "Download";

//cookie
var CookieUser = "user";
var CookieUserName = "user_name";
var CookieEmployeeName = "hrm_employee_name";
var CookieDepId = "hrm_dep_id";
var CookieDepName = "hrm_dep_name";

var url = document.location.href;
var urlString = url.replace("http://" + window.location.host + "/", "");
urlString = urlString.replace("System-HFCDC-Web/", "");


$.getUrlParam = function(name) {
	var reg = new RegExp("(^|&)"+ name +"=([^&]*)(&|$)");
	var r = window.location.search.substr(1).match(reg);
	if (r!=null) {
		return unescape(r[2]);
	} else {
		return null;
	}
}

Date.prototype.Format = function(fmt)
{ //author: meizz
  var o = {
    "M+" : this.getMonth()+1,                 //月份
    "d+" : this.getDate(),                    //日
    "h+" : this.getHours(),                   //小时
    "m+" : this.getMinutes(),                 //分
    "s+" : this.getSeconds(),                 //秒
    "q+" : Math.floor((this.getMonth()+3)/3), //季度
    "S"  : this.getMilliseconds()             //毫秒
  };
  if(/(y+)/.test(fmt))
    fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));
  for(var k in o)
    if(new RegExp("("+ k +")").test(fmt))
  fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));
  return fmt;
}