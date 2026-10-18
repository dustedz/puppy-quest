// Added only in the Android build. The app is already full screen, so hide the game's
// full-screen buttons, and let the app quiet the music when it goes to the background.
(function(){
  try{Object.defineProperty(document,'fullscreenEnabled',{get:function(){return false}});
      Object.defineProperty(document,'webkitFullscreenEnabled',{get:function(){return false}});}catch(e){}
  var ctxs=[],AC=window.AudioContext||window.webkitAudioContext;
  if(AC){var W=function(o){var c=new AC(o);ctxs.push(c);return c};W.prototype=AC.prototype;window.AudioContext=W;window.webkitAudioContext=W;}
  window.__pqPause=function(){ctxs.forEach(function(c){try{c.suspend()}catch(e){}})};
  window.__pqResume=function(){ctxs.forEach(function(c){try{c.resume()}catch(e){}})};
  // Back works like Escape: closes an open menu, leaves photo mode, or opens the pause menu during play.
  // If neither happened (title screen), the app goes to the background.
  window.__pqBack=function(){
    var w=document.getElementById('wrap'),ph=function(){return document.body.classList.contains('photo')},before=w&&!w.hidden||ph();
    dispatchEvent(new KeyboardEvent('keydown',{code:'Escape',key:'Escape'}));
    dispatchEvent(new KeyboardEvent('keyup',{code:'Escape',key:'Escape'}));
    var after=w&&!w.hidden||ph();
    return (before||after)?'ok':'exit';
  };
})();
