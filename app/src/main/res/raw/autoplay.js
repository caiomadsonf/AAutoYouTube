setInterval(function() {
  var ended = document.querySelector('.ytp-next-button');
  var pauseOverlay = document.querySelector('.ytp-pause-overlay');
  if (pauseOverlay) pauseOverlay.remove();
  var autoplayBtn = document.querySelector('button.ytp-autonav-toggle-button');
  if (autoplayBtn && autoplayBtn.getAttribute('aria-checked') === 'false') {
    autoplayBtn.click();
  }
}, 1000);
