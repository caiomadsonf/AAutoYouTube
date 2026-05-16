document.addEventListener('touchstart', function(e) {
  var target = e.target;
  if (target.tagName === 'A' || target.closest('a')) {
    var link = target.tagName === 'A' ? target : target.closest('a');
    var href = link.getAttribute('href');
    if (href && href.includes('/watch')) {
      e.preventDefault();
      window.location.href = 'https://m.youtube.com' + href;
    }
  }
}, true);
