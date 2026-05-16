setTimeout(function() {
  var fullscreenBtn = document.querySelector('.ytp-fullscreen-button');
  if (fullscreenBtn) fullscreenBtn.click();
  if (document.documentElement.requestFullscreen) {
    document.documentElement.requestFullscreen();
  }
}, 2000);
