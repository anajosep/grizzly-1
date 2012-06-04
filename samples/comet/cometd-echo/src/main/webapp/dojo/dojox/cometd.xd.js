/*
	Copyright (c) 2004-2007, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/book/dojo-book-0-9/introduction/licensing
*/

/*
	This is a compiled version of Dojo, built for deployment and not for
	development. To get an editable version, please visit:

		http://dojotoolkit.org

	for documentation and information on getting the source.
*/

dojo._xdResourceLoaded({depends:[["provide","dojo.AdapterRegistry"],["provide","dojo.io.script"],["provide","dojox.cometd._base"],["provide","dojox.cometd"]],defineResource:function(_1){if(!_1._hasResource["dojo.AdapterRegistry"]){_1._hasResource["dojo.AdapterRegistry"]=true;_1.provide("dojo.AdapterRegistry");_1.AdapterRegistry=function(_2){this.pairs=[];this.returnWrappers=_2||false;};_1.extend(_1.AdapterRegistry,{register:function(_3,_4,_5,_6,_7){this.pairs[((_7)?"unshift":"push")]([_3,_4,_5,_6]);},match:function(){for(var i=0;i<this.pairs.length;i++){var _9=this.pairs[i];if(_9[1].apply(this,arguments)){if((_9[3])||(this.returnWrappers)){return _9[2];}else{return _9[2].apply(this,arguments);}}}throw new Error("No match found");},unregister:function(_a){for(var i=0;i<this.pairs.length;i++){var _c=this.pairs[i];if(_c[0]==_a){this.pairs.splice(i,1);return true;}}return false;}});}if(!_1._hasResource["dojo.io.script"]){_1._hasResource["dojo.io.script"]=true;_1.provide("dojo.io.script");_1.io.script={get:function(_d){var _e=this._makeScriptDeferred(_d);var _f=_e.ioArgs;_1._ioAddQueryToUrl(_f);this.attach(_f.id,_f.url);_1._ioWatch(_e,this._validCheck,this._ioCheck,this._resHandle);return _e;},attach:function(id,url){var _12=_1.doc.createElement("script");_12.type="text/javascript";_12.src=url;_12.id=id;_1.doc.getElementsByTagName("head")[0].appendChild(_12);},remove:function(id){_1._destroyElement(_1.byId(id));if(this["jsonp_"+id]){delete this["jsonp_"+id];}},_makeScriptDeferred:function(_14){var dfd=_1._ioSetArgs(_14,this._deferredCancel,this._deferredOk,this._deferredError);var _16=dfd.ioArgs;_16.id="dojoIoScript"+(this._counter++);_16.canDelete=false;if(_14.callbackParamName){_16.query=_16.query||"";if(_16.query.length>0){_16.query+="&";}_16.query+=_14.callbackParamName+"=dojo.io.script.jsonp_"+_16.id+"._jsonpCallback";_16.canDelete=true;dfd._jsonpCallback=this._jsonpCallback;this["jsonp_"+_16.id]=dfd;}return dfd;},_deferredCancel:function(dfd){dfd.canceled=true;if(dfd.ioArgs.canDelete){_1.io.script._deadScripts.push(dfd.ioArgs.id);}},_deferredOk:function(dfd){if(dfd.ioArgs.canDelete){_1.io.script._deadScripts.push(dfd.ioArgs.id);}if(dfd.ioArgs.json){return dfd.ioArgs.json;}else{return dfd.ioArgs;}},_deferredError:function(_19,dfd){if(dfd.ioArgs.canDelete){if(_19.dojoType=="timeout"){_1.io.script.remove(dfd.ioArgs.id);}else{_1.io.script._deadScripts.push(dfd.ioArgs.id);}}console.debug("dojo.io.script error",_19);return _19;},_deadScripts:[],_counter:1,_validCheck:function(dfd){var _1c=_1.io.script;var _1d=_1c._deadScripts;if(_1d&&_1d.length>0){for(var i=0;i<_1d.length;i++){_1c.remove(_1d[i]);}_1.io.script._deadScripts=[];}return true;},_ioCheck:function(dfd){if(dfd.ioArgs.json){return true;}var _20=dfd.ioArgs.args.checkString;if(_20&&eval("typeof("+_20+") != 'undefined'")){return true;}return false;},_resHandle:function(dfd){if(_1.io.script._ioCheck(dfd)){dfd.callback(dfd);}else{dfd.errback(new Error("inconceivable dojo.io.script._resHandle error"));}},_jsonpCallback:function(_22){this.ioArgs.json=_22;}};}if(!_1._hasResource["dojox.cometd._base"]){_1._hasResource["dojox.cometd._base"]=true;_1.provide("dojox.cometd._base");dojox.cometd=new function(){this.DISCONNECTED="DISCONNECTED";this.CONNECTING="CONNECTING";this.CONNECTED="CONNECTED";this.DISCONNECTING="DISCONNECING";this._initialized=false;this._connected=false;this._polling=false;this.connectionTypes=new _1.AdapterRegistry(true);this.version="1.0";this.minimumVersion="0.9";this.clientId=null;this.messageId=0;this.batch=0;this._isXD=false;this.handshakeReturn=null;this.currentTransport=null;this.url=null;this.lastMessage=null;this._messageQ=[];this.handleAs="json-comment-optional";this._advice={};this._maxInterval=30000;this._backoffInterval=1000;this._deferredSubscribes={};this._deferredUnsubscribes={};this._subscriptions=[];this._extendInList=[];this._extendOutList=[];this.state=function(){return this._initialized?(this._connected?this.CONNECTED:this.CONNECTING):(this._connected?this.DISCONNECTING:this.DISCONNECTED);};this.init=function(_23,_24,_25){_24=_24||{};_24.version=this.version;_24.minimumVersion=this.minimumVersion;_24.channel="/meta/handshake";_24.id=""+this.messageId++;this.url=_23||djConfig["cometdRoot"];if(!this.url){console.debug("no cometd root specified in djConfig and no root passed");return null;}var _26="^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?$";var _27=(""+window.location).match(new RegExp(_26));if(_27[4]){var tmp=_27[4].split(":");var _29=tmp[0];var _2a=tmp[1]||"80";_27=this.url.match(new RegExp(_26));if(_27[4]){tmp=_27[4].split(":");var _2b=tmp[0];var _2c=tmp[1]||"80";this._isXD=((_2b!=_29)||(_2c!=_2a));}}if(!this._isXD){if(_24.ext){if(_24.ext["json-comment-filtered"]!==true&&_24.ext["json-comment-filtered"]!==false){_24.ext["json-comment-filtered"]=true;}}else{_24.ext={"json-comment-filtered":true};}}_24=this._extendOut(_24);var _2d={url:this.url,handleAs:this.handleAs,content:{"message":_1.toJson([_24])},load:_1.hitch(this,function(msg){this._finishInit(msg);}),error:_1.hitch(this,function(e){console.debug("handshake error!:",e);this._finishInit([{}]);})};if(_25){_1.mixin(_2d,_25);}this._props=_24;for(var _30 in this._subscriptions){for(var sub in this._subscriptions[_30]){if(this._subscriptions[_30][sub].topic){_1.unsubscribe(this._subscriptions[_30][sub].topic);}}}this._messageQ=[];this._subscriptions=[];this._initialized=true;this.batch=0;this.startBatch();var r;if(this._isXD){_2d.callbackParamName="jsonp";r=_1.io.script.get(_2d);}else{r=_1.xhrPost(_2d);}_1.publish("/cometd/meta",[{cometd:this,action:"handshake",successful:true,state:this.state()}]);return r;};this.publish=function(_33,_34,_35){var _36={data:_34,channel:_33};if(_35){_1.mixin(_36,_35);}this._sendMessage(_36);};this.subscribe=function(_37,_38,_39){if(_38){var _3a="/cometd"+_37;var _3b=this._subscriptions[_3a];if(!_3b||_3b.length==0){_3b=[];this._sendMessage({channel:"/meta/subscribe",subscription:_37});var _ds=this._deferredSubscribes;_ds[_37]=new _1.Deferred();if(_ds[_37]){_ds[_37].cancel();delete _ds[_37];}}for(var i in _3b){if(_3b[i].objOrFunc===_38&&(!_3b[i].funcName&&!_39||_3b[i].funcName==_39)){return null;}}var _3e=_1.subscribe(_3a,_38,_39);_3b.push({topic:_3e,objOrFunc:_38,funcName:_39});this._subscriptions[_3a]=_3b;}return this._deferredSubscribes[_37];};this.unsubscribe=function(_3f,_40,_41){var _42="/cometd"+_3f;var _43=this._subscriptions[_42];if(!_43||_43.length==0){return null;}var s=0;for(var i in _43){var sb=_43[i];if((!_40)||(sb.objOrFunc===_40&&(!sb.funcName&&!_41||sb.funcName==_41))){_1.unsubscribe(_43[i].topic);delete _43[i];}else{s++;}}if(s==0){delete this._subscriptions[_42];this._sendMessage({channel:"/meta/unsubscribe",subscription:_3f});this._deferredUnsubscribes[_3f]=new _1.Deferred();if(this._deferredSubscribes[_3f]){this._deferredSubscribes[_3f].cancel();delete this._deferredSubscribes[_3f];}}return this._deferredUnsubscribes[_3f];};this.disconnect=function(){for(var _47 in this._subscriptions){for(var sub in this._subscriptions[_47]){if(this._subscriptions[_47][sub].topic){_1.unsubscribe(this._subscriptions[_47][sub].topic);}}}this._subscriptions=[];this._messageQ=[];if(this._initialized&&this.currentTransport){this._initialized=false;this.currentTransport.disconnect();}if(!this._polling){this._connected=false;_1.publish("/cometd/meta",[{cometd:this,action:"connect",successful:false,state:this.state()}]);}this._initialized=false;_1.publish("/cometd/meta",[{cometd:this,action:"disconnect",successful:true,state:this.state()}]);};this.subscribed=function(_49,_4a){};this.unsubscribed=function(_4b,_4c){};this.tunnelInit=function(_4d,_4e){};this.tunnelCollapse=function(){};this._backoff=function(){if(!this._advice||!this._advice.interval){this._advice={reconnect:"retry",interval:0};}if(this._advice.interval<this._maxInterval){this._advice.interval+=this._backoffInterval;}};this._finishInit=function(_4f){_4f=_4f[0];this.handshakeReturn=_4f;if(_4f["advice"]){this._advice=_4f.advice;}var _50=_4f.successful?_4f.successful:false;if(_4f.version<this.minimumVersion){console.debug("cometd protocol version mismatch. We wanted",this.minimumVersion,"but got",_4f.version);_50=false;this._advice.reconnect="none";}if(_50){this.currentTransport=this.connectionTypes.match(_4f.supportedConnectionTypes,_4f.version,this._isXD);this.currentTransport._cometd=this;this.currentTransport.version=_4f.version;this.clientId=_4f.clientId;this.tunnelInit=_1.hitch(this.currentTransport,"tunnelInit");this.tunnelCollapse=_1.hitch(this.currentTransport,"tunnelCollapse");this.currentTransport.startup(_4f);}_1.publish("/cometd/meta",[{cometd:this,action:"handshook",successful:_50,state:this.state()}]);if(!_50){console.debug("cometd init failed");this._backoff();if(this._advice&&this._advice["reconnect"]=="none"){console.debug("cometd reconnect: none");}else{if(this._advice&&this._advice["interval"]&&this._advice.interval>0){setTimeout(_1.hitch(this,function(){this.init(cometd.url,this._props);}),this._advice.interval);}else{this.init(this.url,this._props);}}}};this._extendIn=function(_51){var m=_51;_1.forEach(dojox.cometd._extendInList,function(f){var n=f(m);if(n){m=n;}});return m;};this._extendOut=function(_55){var m=_55;_1.forEach(dojox.cometd._extendOutList,function(f){var n=f(m);if(n){m=n;}});return m;};this.deliver=function(_59){_1.forEach(_59,this._deliver,this);return _59;};this._deliver=function(_5a){_5a=this._extendIn(_5a);if(!_5a["channel"]){if(_5a["success"]!==true){console.debug("cometd error: no channel for message!",_5a);return;}}this.lastMessage=_5a;if(_5a.advice){this._advice=_5a.advice;}var _5b=null;if((_5a["channel"])&&(_5a.channel.length>5)&&(_5a.channel.substr(0,5)=="/meta")){switch(_5a.channel){case "/meta/connect":if(_5a.successful&&!this._connected){this._connected=this._initialized;this.endBatch();}else{if(!this._initialized){this._connected=false;}}_1.publish("/cometd/meta",[{cometd:this,action:"connect",successful:_5a.successful,state:this.state()}]);break;case "/meta/subscribe":_5b=this._deferredSubscribes[_5a.subscription];if(!_5a.successful){if(_5b){_5b.errback(new Error(_5a.error));}return;}dojox.cometd.subscribed(_5a.subscription,_5a);if(_5b){_5b.callback(true);}break;case "/meta/unsubscribe":_5b=this._deferredUnsubscribes[_5a.subscription];if(!_5a.successful){if(_5b){_5b.errback(new Error(_5a.error));}return;}this.unsubscribed(_5a.subscription,_5a);if(_5b){_5b.callback(true);}break;}}this.currentTransport.deliver(_5a);if(_5a.data){try{var _5c="/cometd"+_5a.channel;_1.publish(_5c,[_5a]);}catch(e){console.debug(e);}}};this._sendMessage=function(_5d){if(this.currentTransport&&this._connected&&this.batch==0){return this.currentTransport.sendMessages([_5d]);}else{this._messageQ.push(_5d);return null;}};this.startBatch=function(){this.batch++;};this.endBatch=function(){if(--this.batch<=0&&this.currentTransport&&this._connected){this.batch=0;var _5e=this._messageQ;this._messageQ=[];if(_5e.length>0){this.currentTransport.sendMessages(_5e);}}};this._onUnload=function(){_1.addOnUnload(dojox.cometd,"disconnect");};};dojox.cometd.longPollTransport=new function(){this._connectionType="long-polling";this._cometd=null;this.check=function(_5f,_60,_61){return ((!_61)&&(_1.indexOf(_5f,"long-polling")>=0));};this.tunnelInit=function(){var _62={channel:"/meta/connect",clientId:this._cometd.clientId,connectionType:this._connectionType,id:""+this._cometd.messageId++};_62=this._cometd._extendOut(_62);this.openTunnelWith({message:_1.toJson([_62])});};this.tunnelCollapse=function(){if(!this._cometd._initialized){return;}if(this._cometd._advice){if(this._cometd._advice["reconnect"]=="none"){return;}if((this._cometd._advice["interval"])&&(this._cometd._advice.interval>0)){setTimeout(_1.hitch(this,function(){this._connect();}),this._cometd._advice.interval);}else{this._connect();}}else{this._connect();}};this._connect=function(){if(!this._cometd._initialized){return;}if(this._cometd._polling){console.debug("wait for poll to complete or fail");return;}if((this._cometd._advice)&&(this._cometd._advice["reconnect"]=="handshake")){this._cometd._connected=false;this._initialized=false;this._cometd.init(this._cometd.url,this._cometd._props);}else{if(this._cometd._connected){var _63={channel:"/meta/connect",connectionType:this._connectionType,clientId:this._cometd.clientId,id:""+this._cometd.messageId++};_63=this._cometd._extendOut(_63);this.openTunnelWith({message:_1.toJson([_63])});}}};this.deliver=function(_64){};this.openTunnelWith=function(_65,url){this._cometd._polling=true;var d=_1.xhrPost({url:(url||this._cometd.url),content:_65,handleAs:this._cometd.handleAs,load:_1.hitch(this,function(_68){this._cometd._polling=false;this._cometd.deliver(_68);this.tunnelCollapse();}),error:_1.hitch(this,function(err){this._cometd._polling=false;console.debug("tunnel opening failed:",err);_1.publish("/cometd/meta",[{cometd:this._cometd,action:"connect",successful:false,state:this._cometd.state()}]);this._cometd._backoff();this.tunnelCollapse();})});};this.sendMessages=function(_6a){for(var i=0;i<_6a.length;i++){_6a[i].clientId=this._cometd.clientId;_6a[i].id=""+this._cometd.messageId++;_6a[i]=this._cometd._extendOut(_6a[i]);}return _1.xhrPost({url:this._cometd.url||djConfig["cometdRoot"],handleAs:this._cometd.handleAs,load:_1.hitch(this._cometd,"deliver"),content:{message:_1.toJson(_6a)}});};this.startup=function(_6c){if(this._cometd._connected){return;}this.tunnelInit();};this.disconnect=function(){var _6d={channel:"/meta/disconnect",clientId:this._cometd.clientId,id:""+this._cometd.messageId++};_6d=this._cometd._extendOut(_6d);_1.xhrPost({url:this._cometd.url||djConfig["cometdRoot"],handleAs:this._cometd.handleAs,content:{message:_1.toJson([_6d])}});};};dojox.cometd.callbackPollTransport=new function(){this._connectionType="callback-polling";this._cometd=null;this.check=function(_6e,_6f,_70){return (_1.indexOf(_6e,"callback-polling")>=0);};this.tunnelInit=function(){var _71={channel:"/meta/connect",clientId:this._cometd.clientId,connectionType:this._connectionType,id:""+this._cometd.messageId++};_71=this._cometd._extendOut(_71);this.openTunnelWith({message:_1.toJson([_71])});};this.tunnelCollapse=dojox.cometd.longPollTransport.tunnelCollapse;this._connect=dojox.cometd.longPollTransport._connect;this.deliver=dojox.cometd.longPollTransport.deliver;this.openTunnelWith=function(_72,url){this._cometd._polling=true;_1.io.script.get({load:_1.hitch(this,function(_74){this._cometd._polling=false;this._cometd.deliver(_74);this.tunnelCollapse();}),error:_1.hitch(this,function(err){this._cometd._polling=false;console.debug("tunnel opening failed:",err);_1.publish("/cometd/meta",[{cometd:this._cometd,action:"connect",successful:false,state:this._cometd.state()}]);this._cometd._backoff();this.tunnelCollapse();}),url:(url||this._cometd.url),content:_72,callbackParamName:"jsonp"});};this.sendMessages=function(_76){for(var i=0;i<_76.length;i++){_76[i].clientId=this._cometd.clientId;_76[i].id=""+this._cometd.messageId++;_76[i]=this._cometd._extendOut(_76[i]);}var _78={url:this._cometd.url||djConfig["cometdRoot"],load:_1.hitch(this._cometd,"deliver"),callbackParamName:"jsonp",content:{message:_1.toJson(_76)}};return _1.io.script.get(_78);};this.startup=function(_79){if(this._cometd._connected){return;}this.tunnelInit();};this.disconnect=dojox.cometd.longPollTransport.disconnect;this.disconnect=function(){var _7a={channel:"/meta/disconnect",clientId:this._cometd.clientId,id:""+this._cometd.messageId++};_7a=this._cometd._extendOut(_7a);_1.io.script.get({url:this._cometd.url||djConfig["cometdRoot"],callbackParamName:"jsonp",content:{message:_1.toJson([_7a])}});};};dojox.cometd.connectionTypes.register("long-polling",dojox.cometd.longPollTransport.check,dojox.cometd.longPollTransport);dojox.cometd.connectionTypes.register("callback-polling",dojox.cometd.callbackPollTransport.check,dojox.cometd.callbackPollTransport);_1.addOnUnload(dojox.cometd,"_onUnload");}if(!_1._hasResource["dojox.cometd"]){_1._hasResource["dojox.cometd"]=true;_1.provide("dojox.cometd");}}});